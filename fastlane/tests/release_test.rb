require "minitest/autorun"
require "minitest/mock"
require "tmpdir"
require "uri"

# Load the real Fastfile against an in-memory action boundary. No network,
# Gradle, signing material, or real release files are used by these tests.
class ReleaseHarness
  module UI
    def self.user_error!(message)
      raise message
    end

    def self.message(*)
    end

    def self.important(*)
    end
  end

  def self.default_platform(*)
  end

  def self.skip_docs
  end

  def self.desc(*)
  end

  def self.platform(*)
    yield
  end

  def self.lane(name, &block)
    define_method(name, &block)
  end

  fastfile = File.expand_path("../Fastfile", __dir__)
  class_eval(File.read(fastfile), fastfile)

  attr_accessor :remote_release, :remote_tag_sha, :play_codes, :fail_upload,
                :lookup_status, :creation_fails, :tag_lookup_status, :commit_lookup_status,
                :remote_tag_type
  attr_reader :uploads, :deletions, :play_uploads, :creations, :calls

  def initialize(artifact_dir)
    @paths = { play_apk: File.join(artifact_dir, "app-play-release.apk"),
               oss_apk: File.join(artifact_dir, "app-oss-release.apk"),
               play_aab: File.join(artifact_dir, "app-play-release.aab") }
    @paths.each_value { |path| File.binwrite(path, "test artifact") }
    @play_codes = []
    @uploads = []
    @deletions = []
    @play_uploads = []
    @creations = []
    @calls = []
    @remote_tag_type = "commit"
    @next_asset_id = 10
  end

  def current_version_code
    22
  end

  def current_version_name
    "1.6.0"
  end

  def checkout_commit
    "a" * 40
  end

  def release_artifacts
    @paths
  end

  def ensure_release_notes!
  end

  def play_credentials
    { json_key_data: "test credentials" }
  end

  def google_play_track_version_codes(**)
    @play_codes
  end

  def google_play_track_release_names(**)
    raise "Release display names must not be used to identify an uploaded bundle"
  end

  def upload_to_play_store(**options)
    @play_uploads << options
    @play_codes << options.fetch(:version_code)
  end

  def set_github_release(**options)
    @creations << options
    return nil if @creation_fails

    raise "Attempted to recreate an existing release" if @remote_release
    @remote_tag_sha ||= options.fetch(:commitish)
    @remote_release = { "id" => 1, "tag_name" => options.fetch(:tag_name), "draft" => options.fetch(:is_draft, false), "assets" => [] }
    copy(@remote_release)
  end

  def github_api(**options)
    @calls << options
    path = options[:path].to_s
    method = options.fetch(:http_method)
    status = 200
    json = {}
    if method == "GET" && path.include?("/releases/tags/")
      status = @lookup_status || (@remote_release ? 200 : 404)
      json = @remote_release || {}
    elsif method == "GET" && path.include?("/git/ref/tags/")
      status = @tag_lookup_status || (@remote_tag_sha ? 200 : 404)
      json = { "object" => { "type" => @remote_tag_type, "sha" => @remote_tag_type == "tag" ? "c" * 40 : @remote_tag_sha } }
    elsif method == "GET" && path.include?("/commits/")
      status = @commit_lookup_status || (@remote_tag_sha ? 200 : 422)
      json = { "sha" => @remote_tag_sha }
    elsif method == "DELETE"
      id = path.split("/").last.to_i
      @deletions << id
      @remote_release["assets"].reject! { |asset| asset["id"] == id }
      status = 204
    elsif method == "POST" && options[:url]
      name = URI.decode_www_form(URI(options[:url]).query).to_h.fetch("name")
      @uploads << name
      raise "Duplicate asset upload" if @remote_release["assets"].any? { |asset| asset["name"] == name }

      @next_asset_id += 1
      failed = name == @fail_upload
      json = { "id" => @next_asset_id, "name" => name,
               "state" => failed ? "starter" : "uploaded",
               "size" => failed ? 0 : options.fetch(:raw_body).bytesize }
      @remote_release["assets"] << json
      status = failed ? 502 : 201
    elsif method == "PATCH"
      raise "Published before both APKs were uploaded" unless ReleaseHarness::RELEASE_ASSET_NAMES.all? do |name|
        @remote_release["assets"].any? { |asset| asset["name"] == name && uploaded_release_asset?(asset) }
      end
      @remote_release["draft"] = options.fetch(:body).fetch(:draft)
      json = @remote_release
    else
      raise "Unexpected GitHub action: #{method} #{path}"
    end
    response = { status: status, json: copy(json) }
    if status >= 400
      handler = options.fetch(:error_handlers)[status] || options.fetch(:error_handlers).fetch("*")
      handler.call(response)
    end
    response
  end

  def copy(value)
    Marshal.load(Marshal.dump(value))
  end
end

class ReleaseRetryTest < Minitest::Test
  def setup
    @env = %w[GITHUB_TOKEN GITHUB_REPOSITORY RELEASE_COMMIT_SHA GITHUB_OUTPUT].to_h { |key| [key, ENV[key]] }
    ENV["GITHUB_TOKEN"] = "test-token-not-a-real-credential"
    ENV["GITHUB_REPOSITORY"] = "example/scrolless"
    ENV.delete("RELEASE_COMMIT_SHA")
    @tmp = Dir.mktmpdir("scrolless-release-test-")
    ENV["GITHUB_OUTPUT"] = File.join(@tmp, "outputs")
    @lane = ReleaseHarness.new(@tmp)
  end

  def teardown
    @env.each { |key, value| value.nil? ? ENV.delete(key) : ENV[key] = value }
    FileUtils.remove_entry(@tmp)
  end

  def asset(name, state: "uploaded", size: 12, id: 2)
    { "id" => id, "name" => name, "state" => state, "size" => size }
  end

  def existing_release(assets = [], draft: false)
    @lane.remote_tag_sha = @lane.checkout_commit
    @lane.remote_release = { "id" => 1, "draft" => draft, "assets" => assets }
    @lane.play_codes = [22]
  end

  def output
    File.read(ENV.fetch("GITHUB_OUTPUT"))
  end

  def test_play_retry_uses_version_code_without_looking_up_display_names
    @lane.play_codes = [21, 22]
    @lane.publish_internal({})
    assert_empty @lane.play_uploads
  end

  def test_new_play_version_is_uploaded_with_changelogs
    @lane.play_codes = [21]
    @lane.publish_internal({})
    assert_equal 1, @lane.play_uploads.size
    assert_equal 22, @lane.play_uploads.first[:version_code]
    assert_equal false, @lane.play_uploads.first[:skip_upload_changelogs]
  end

  def test_partial_upload_failure_is_resumable_without_reuploading_play_or_healthy_assets
    @lane.fail_upload = "app-oss-release.apk"
    assert_raises(RuntimeError) { @lane.publish_release({}) }
    assert_equal 1, @lane.creations.size
    assert_equal true, @lane.creations.first[:is_draft]
    assert_equal true, @lane.remote_release["draft"]
    refute @lane.calls.any? { |call| call[:http_method] == "PATCH" }
    assert_equal 1, @lane.play_uploads.size
    assert_equal "starter", @lane.remote_release["assets"].last["state"]
    failed_id = @lane.remote_release["assets"].last["id"]
    @lane.release_status
    assert_includes output, "should_release=true"

    @lane.fail_upload = nil
    @lane.publish_release({})
    assert_equal false, @lane.remote_release["draft"]
    assert_equal 1, @lane.creations.size
    assert_equal 1, @lane.play_uploads.size
    assert_equal ["app-play-release.apk", "app-oss-release.apk", "app-oss-release.apk"], @lane.uploads
    assert_equal [failed_id], @lane.deletions
    @lane.release_status
    assert output.end_with?("version_name=1.6.0\n")
    assert_includes output, "should_release=false"
  end

  def test_existing_tag_without_release_does_not_skip
    @lane.remote_tag_sha = @lane.checkout_commit
    @lane.release_status
    assert_includes output, "should_release=true"
    @lane.publish_release({})
    assert_equal 1, @lane.creations.size
    assert_equal 2, @lane.uploads.size
  end

  def test_new_release_checks_tag_existence_without_resolving_a_missing_commit
    @lane.release_status
    assert_includes output, "should_release=true"
    assert @lane.calls.any? { |call| call[:path] == "repos/example/scrolless/git/ref/tags/v1.6.0" }
    refute @lane.calls.any? { |call| call[:path].to_s.include?("/commits/") }
  end

  def test_tag_lookup_errors_are_not_treated_as_an_absent_tag
    [401, 403, 422, 500].each do |status|
      @lane.tag_lookup_status = status
      error = assert_raises(RuntimeError) { @lane.release_status }
      assert_includes error.message, "HTTP #{status}"
      refute File.exist?(ENV["GITHUB_OUTPUT"])
    end
  end

  def test_existing_tag_commit_resolution_errors_stop_the_release
    @lane.remote_tag_sha = @lane.checkout_commit
    [404, 422].each do |status|
      @lane.commit_lookup_status = status
      assert_raises(RuntimeError) { @lane.publish_release({}) }
      assert_empty @lane.play_uploads
      assert_empty @lane.creations
    end
  end

  def test_annotated_tag_is_compared_using_its_commit_not_its_tag_object
    @lane.remote_tag_type = "tag"
    @lane.remote_tag_sha = @lane.checkout_commit
    @lane.release_status
    assert_includes output, "should_release=true"
    assert @lane.calls.any? { |call| call[:path].to_s.include?("/commits/") }
  end

  def test_existing_release_with_no_assets_is_repaired
    existing_release
    @lane.publish_release({})
    assert_empty @lane.creations
    assert_empty @lane.play_uploads
    assert_equal ["app-play-release.apk", "app-oss-release.apk"], @lane.uploads
  end

  def test_existing_release_with_one_missing_asset_only_uploads_that_asset
    existing_release([asset("app-play-release.apk")])
    @lane.publish_release({})
    assert_equal ["app-oss-release.apk"], @lane.uploads
    assert_empty @lane.deletions
  end

  def test_complete_release_skips_without_requiring_original_commit_or_play_access
    existing_release([asset("app-play-release.apk"), asset("app-oss-release.apk", id: 3)])
    @lane.remote_tag_sha = "b" * 40
    @lane.release_status
    assert_includes output, "should_release=false"
    @lane.publish_release({})
    assert_empty @lane.play_uploads
    assert_empty @lane.uploads
    assert_empty @lane.creations
  end

  def test_unrelated_assets_do_not_count_as_completion
    existing_release([asset("source.zip"), asset("notes.txt", id: 3)])
    @lane.release_status
    assert_includes output, "should_release=true"
  end

  def test_zero_byte_uploaded_asset_is_replaced_without_deleting_healthy_asset
    existing_release([asset("app-play-release.apk"), asset("app-oss-release.apk", size: 0, id: 3)])
    @lane.publish_release({})
    assert_equal [3], @lane.deletions
    assert_equal ["app-oss-release.apk"], @lane.uploads
  end

  def test_auth_and_server_errors_are_not_treated_as_missing_releases
    [401, 403, 500].each do |status|
      @lane.lookup_status = status
      error = assert_raises(RuntimeError) { @lane.release_status }
      assert_includes error.message, "HTTP #{status}"
      refute_includes error.message, ENV["GITHUB_TOKEN"]
      refute File.exist?(ENV["GITHUB_OUTPUT"])
    end
  end

  def test_incomplete_release_cannot_receive_assets_from_another_commit
    existing_release([asset("app-play-release.apk")])
    @lane.remote_tag_sha = "b" * 40
    assert_raises(RuntimeError) { @lane.release_status }
    assert_raises(RuntimeError) { @lane.publish_release({}) }
    assert_empty @lane.play_uploads
    assert_empty @lane.uploads
  end

  def test_explicit_release_sha_must_match_checkout
    ENV["RELEASE_COMMIT_SHA"] = "b" * 40
    assert_raises(RuntimeError) { @lane.publish_release({}) }
    assert_empty @lane.creations
    assert_empty @lane.play_uploads
  end

  def test_draft_is_not_complete_and_is_published_after_assets_are_present
    existing_release([asset("app-play-release.apk"), asset("app-oss-release.apk", id: 3)], draft: true)
    @lane.release_status
    assert_includes output, "should_release=true"
    @lane.publish_release({})
    assert_equal false, @lane.remote_release["draft"]
    assert_empty @lane.uploads
  end

  def test_failed_release_creation_is_not_reported_as_success
    @lane.creation_fails = true
    assert_raises(RuntimeError) { @lane.publish_release({}) }
    assert_empty @lane.uploads
  end
end

class FastlanePreflightIntegrationTest < Minitest::Test
  def test_real_fastlane_action_wiring_handles_complete_and_missing_releases
    require "fastlane"
    require "fastlane/actions/github_api"
    require "excon"
    Fastlane.load_actions

    saved_env = %w[GITHUB_TOKEN GITHUB_REPOSITORY RELEASE_COMMIT_SHA GITHUB_OUTPUT].to_h { |key| [key, ENV[key]] }
    ENV["GITHUB_TOKEN"] = "test-token-not-a-real-credential"
    ENV["GITHUB_REPOSITORY"] = "example/scrolless"
    ENV.delete("RELEASE_COMMIT_SHA")
    release = { "draft" => false, "assets" => %w[app-play-release.apk app-oss-release.apk].map do |name|
      { "name" => name, "state" => "uploaded", "size" => 1 }
    end }
    transport = lambda do |url, method, _headers, _body, _secure|
      raise "Unexpected network operation" unless method == "GET" && url.start_with?("https://api.github.com/repos/example/scrolless/")

      if url.include?("/releases/tags/")
        Excon::Response.new(status: release ? 200 : 404, body: JSON.generate(release || {}))
      elsif url.include?("/git/ref/tags/")
        Excon::Response.new(status: 404, body: "{}")
      elsif url.include?("/commits/")
        Excon::Response.new(status: 422, body: JSON.generate({ "message" => "No commit found for SHA" }))
      else
        raise "Unexpected GitHub endpoint"
      end
    end
    Dir.mktmpdir("fastlane-preflight-test-") do |tmp|
      ENV["GITHUB_OUTPUT"] = File.join(tmp, "outputs")
      file = File.expand_path("../Fastfile", __dir__)
      fastfile = Fastlane::FastFile.new.parse(File.read(file), file)
      Fastlane::Actions::GithubApiAction.stub(:call_endpoint, transport) do
        fastfile.runner.execute(:release_status, :android)
        assert_includes File.read(ENV["GITHUB_OUTPUT"]), "should_release=false"
        release = nil
        fastfile.runner.execute(:release_status, :android)
        assert_includes File.read(ENV["GITHUB_OUTPUT"]), "should_release=true"
      end
    end
  ensure
    saved_env&.each { |key, value| value.nil? ? ENV.delete(key) : ENV[key] = value }
  end
end
