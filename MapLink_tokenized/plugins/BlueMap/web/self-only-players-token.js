(function(){
  function getParam(name){
    try{ return new URL(window.location.href).searchParams.get(name); }
    catch(e){ return null; }
  }

  const token = getParam('token');
  const playerParam = getParam('player');

  // Si player (UUID) en clair est fourni, on l'utilise (compatibilité)
  if(playerParam){
    window.__bluemap_target_uuid = playerParam.toLowerCase();
    hideOthersWhenReady();
    return;
  }

  if(!token){
    console.log('[BlueMap MapLink] pas de token ni player dans l\'URL, script inactif.');
    return;
  }

  // chemin vers le fichier JSON (même dossier que ce script)
  const tokenJsonPath = 'maplink_tokens.json'; // si tu changes l'emplacement, modifie ici

  fetch(tokenJsonPath).then(r=>{
    if(!r.ok) throw new Error('Token file non accessible: ' + r.status);
    return r.json();
  }).then(obj=>{
    if(!obj[token]){
      console.warn('[BlueMap MapLink] Token introuvable ou expiré');
      return;
    }
    const uuid = obj[token].uuid.toLowerCase();
    window.__bluemap_target_uuid = uuid;
    hideOthersWhenReady();
  }).catch(e=>{
    console.warn('[BlueMap MapLink] Erreur fetch token file', e);
  });

  function hideOthersWhenReady(attemptsLeft=50){
    if(typeof api !== 'undefined' && api.getWebApp){
      try{
        const webapp = api.getWebApp();
        if(webapp && typeof webapp.getPlayers === 'function' && typeof webapp.setPlayerVisibility === 'function'){
          webapp.getPlayers().forEach(p => {
            const uuid = (p.uuid || '').toLowerCase();
            webapp.setPlayerVisibility(uuid, uuid === window.__bluemap_target_uuid);
          });
          return;
        }
      }catch(e){/* fallback DOM */}
    }

    // Fallback DOM selectors - adapte si ta version diffère
    document.querySelectorAll('[data-uuid], .player-marker, .bm-player').forEach(el => {
      const id = (el.getAttribute('data-uuid') || el.dataset.uuid || '').toLowerCase();
      if(id && id !== window.__bluemap_target_uuid) el.style.display = 'none';
    });

    if(attemptsLeft>0) setTimeout(()=> hideOthersWhenReady(attemptsLeft-1), 200);
  }

})();
