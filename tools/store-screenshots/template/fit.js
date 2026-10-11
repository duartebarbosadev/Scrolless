  // Shrinks the headline (and later the subtitle) until every line fits the text box.
  const box=document.querySelector('.text'), h=box.querySelector('h1'), s=box.querySelector('p.sub');
  let hs={{headline_size}}, ss={{subtitle_size}};
  const ok=()=>{
    const wide=[...h.querySelectorAll('span')].every(e=>e.scrollWidth<=box.clientWidth+1) && s.scrollWidth<=box.clientWidth+1;
    return wide && box.scrollHeight<=box.clientHeight;
  };
  const apply=()=>{h.style.fontSize=hs+'px'; s.style.fontSize=ss+'px';};
  apply();
  while(!ok() && hs>{{headline_min}}){ hs-=2; if(hs<{{shrink_subtitle_below}} && ss>{{subtitle_min}}) ss-=1; apply(); }
  if(!ok()) document.title='overflow';
