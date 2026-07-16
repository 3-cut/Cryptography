// =====================================================================
// Navigation (équivalent du CardLayout/MainFrame côté Swing)
// =====================================================================

const initialisateurs = {
  sieve: initSieve,
  factorization: () => initFactorization(0),
  'rsa-protocol': initRsaProtocol,
  'lattice-standard': () => initLattice('screen-lattice-standard', 'standard'),
  'lattice-combination': () => initLattice('screen-lattice-combination', 'combination'),
  'lattice-cvp': () => initLattice('screen-lattice-cvp', 'cvp'),
  'kyber-protocol': initKyberProtocol,
};

function allerVers(nom) {
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('actif'));
  document.getElementById('screen-' + nom).classList.add('actif');
  if (initialisateurs[nom]) {
    initialisateurs[nom]();
  }
}

document.addEventListener('DOMContentLoaded', () => allerVers('home'));

// =====================================================================
// Petites aides communes
// =====================================================================

async function postForm(url, params) {
  const body = new URLSearchParams(params || {});
  const res = await fetch(url, { method: 'POST', body });
  return res.json();
}

async function getJson(url) {
  const res = await fetch(url);
  return res.json();
}

function formatGrand(valeurStr) {
  if (valeurStr.length <= 50) return valeurStr;
  return valeurStr.substring(0, 24) + '…(' + valeurStr.length + ' chiffres)…' + valeurStr.substring(valeurStr.length - 10);
}

function infoDialog(titre, html) {
  document.getElementById('popup-titre').textContent = titre;
  document.getElementById('popup-contenu').textContent = html;
  document.getElementById('popup-overlay').classList.add('actif');
}

function fermerPopup(event) {
  if (event && event.target !== document.getElementById('popup-overlay')) return;
  document.getElementById('popup-overlay').classList.remove('actif');
}

// =====================================================================
// CRIBLE D'ÉRATOSTHÈNE
// =====================================================================

let sieveModeSelection = true;
let sievePrimes = [];
let sieveMax = 50;
let sieveSelection = {};
let sieveTermine = false;
let sieveNonPremiersTraites = 0;
let sieveTotalNonPremiers = 0;

async function initSieve() {
  const data = await getJson('/api/sieve');
  sievePrimes = data.primes;
  sieveMax = data.max;
  sieveSelection = {};
  sieveTermine = false;
  sieveNonPremiersTraites = 0;
  sieveTotalNonPremiers = sieveMax - sievePrimes.length;

  document.getElementById('sieve-soustitre').textContent = sieveModeSelection
    ? 'Sélectionnez les nombres que vous pensez premiers, puis validez.'
    : 'Trouvez les nombres premiers et supprimez leurs multiples.';
  document.getElementById('sieve-valider').style.display = sieveModeSelection ? 'inline-block' : 'none';
  document.getElementById('sieve-valider').textContent = 'Valider';
  document.getElementById('sieve-message').textContent = '';

  const grille = document.getElementById('sieve-grille');
  grille.innerHTML = '';
  for (let n = 1; n <= sieveMax; n++) {
    const bouton = document.createElement('button');
    bouton.textContent = n;
    bouton.dataset.n = n;
    if (sieveModeSelection) {
      bouton.onclick = () => sieveToggle(n, bouton);
    } else {
      bouton.onclick = () => sieveClicClassique(n, bouton);
    }
    grille.appendChild(bouton);
  }
}

function estPremierJs(n) {
  return sievePrimes.includes(n);
}

function sieveToggle(n, bouton) {
  if (sieveTermine) return;
  sieveSelection[n] = !sieveSelection[n];
  bouton.classList.toggle('selectionne', !!sieveSelection[n]);
}

function sieveValider() {
  if (sieveTermine) {
    allerVers('factorization');
    return;
  }
  sieveTermine = true;
  let parfait = true;
  document.querySelectorAll('#sieve-grille button').forEach(bouton => {
    const n = parseInt(bouton.dataset.n, 10);
    const choisi = !!sieveSelection[n];
    const premier = n !== 1 && estPremierJs(n);
    bouton.disabled = true;
    bouton.classList.remove('selectionne');
    if (choisi && premier) {
      bouton.classList.add('premier');
    } else if (choisi && !premier) {
      bouton.classList.add('erreur');
      parfait = false;
    } else if (!choisi && premier) {
      bouton.classList.add('bleu');
      parfait = false;
    }
  });
  const message = document.getElementById('sieve-message');
  if (parfait) {
    message.style.color = 'var(--couleur-succes)';
    message.textContent = 'Bien joué ! Tous les nombres premiers ont été correctement sélectionnés.';
  } else {
    message.style.color = 'var(--couleur-erreur)';
    message.textContent = 'Il y a des erreurs : vert = premier sélectionné, rouge = non premier sélectionné, bleu = premier non sélectionné.';
  }
  document.getElementById('sieve-valider').textContent = 'Suite';
}

function sieveClicClassique(n, bouton) {
  if (bouton.disabled || sieveTermine) return;
  const message = document.getElementById('sieve-message');
  if (n === 1) {
    marquerSupprimeJs(bouton, ' ');
    message.style.color = '#444';
    message.textContent = "1 n'est ni premier ni composé : il est retiré du crible.";
    sieveNonPremiersTraites++;
  } else if (estPremierJs(n)) {
    bouton.classList.add('premier');
    bouton.disabled = true;
    griserMultiplesJs(n);
    message.style.color = 'var(--couleur-premier)';
    message.textContent = n + ' est un nombre premier : ses multiples sont supprimés.';
  } else {
    marquerSupprimeJs(bouton, String(n));
    message.style.color = '#444';
    message.textContent = n + ' est un multiple d\'un nombre premier : supprimé du crible.';
    sieveNonPremiersTraites++;
  }
  verifierFinSieve();
}

function griserMultiplesJs(premier) {
  for (let m = 2 * premier; m <= sieveMax; m += premier) {
    const bouton = document.querySelector('#sieve-grille button[data-n="' + m + '"]');
    if (bouton && !bouton.disabled) {
      marquerSupprimeJs(bouton, String(m));
      sieveNonPremiersTraites++;
    }
  }
}

function marquerSupprimeJs(bouton, texte) {
  bouton.textContent = texte;
  bouton.classList.add('supprime');
  bouton.disabled = true;
}

function verifierFinSieve() {
  if (sieveTermine || sieveNonPremiersTraites < sieveTotalNonPremiers) return;
  sieveTermine = true;
  const message = document.getElementById('sieve-message');
  message.style.color = 'var(--couleur-succes)';
  message.textContent = 'Bravo, vous avez terminé le crible ! Tous les nombres non premiers sont supprimés.';
  setTimeout(() => allerVers('factorization'), 1800);
}

function changerModeCrible() {
  sieveModeSelection = !sieveModeSelection;
  initSieve();
}

function afficherInfoPremier() {
  infoDialog('Définition', 'Un nombre premier est un entier naturel admettant exactement 2 diviseurs : 1 et lui-même.');
}

// =====================================================================
// CHALLENGE DE FACTORISATION
// =====================================================================

let factoLevelIndex = 0;
let factoN = '';
let factoValidated = false;
let factoTotalNiveaux = 4;

async function initFactorization(niveau) {
  factoLevelIndex = niveau;
  factoValidated = false;
  const data = await getJson('/api/factorization/level?level=' + niveau);
  factoN = data.n;
  factoTotalNiveaux = data.totalNiveaux || 4;
  const dernierNiveau = niveau === factoTotalNiveaux - 1;

  document.getElementById('facto-niveau').textContent =
    'Niveau ' + (niveau + 1) + ' / ' + factoTotalNiveaux;
  document.getElementById('facto-n').textContent = 'N = ' + formatGrand(factoN);
  document.getElementById('facto-p').value = '';
  document.getElementById('facto-q').value = '';
  document.getElementById('facto-p').disabled = false;
  document.getElementById('facto-q').disabled = false;
  document.getElementById('facto-p').style.borderColor = 'var(--bord)';
  document.getElementById('facto-q').style.borderColor = 'var(--bord)';
  document.getElementById('facto-message').textContent = '';
  document.getElementById('facto-valider').textContent = 'Valider';
  document.getElementById('facto-giveup').style.display = dernierNiveau ? 'inline-block' : 'none';
  document.getElementById('facto-giveup').disabled = false;
  document.getElementById('facto-voir-entier').style.display = dernierNiveau ? 'inline-block' : 'none';
}

async function factoValider() {
  if (!factoValidated) {
    const p = document.getElementById('facto-p').value.trim();
    const q = document.getElementById('facto-q').value.trim();
    const data = await postForm('/api/factorization/validate', { p, q });

    factoValidated = true;
    document.getElementById('facto-p').disabled = true;
    document.getElementById('facto-q').disabled = true;
    document.getElementById('facto-giveup').disabled = true;
    const dernierNiveau = factoLevelIndex === factoTotalNiveaux - 1;
    document.getElementById('facto-valider').textContent = dernierNiveau ? 'Suite (protocole RSA)' : 'Suite';

    const message = document.getElementById('facto-message');
    if (data.correct) {
      message.style.color = 'var(--couleur-succes)';
      message.textContent = 'Bien joué !';
    } else {
      document.getElementById('facto-p').value = data.solutionP;
      document.getElementById('facto-q').value = data.solutionQ;
      document.getElementById('facto-p').style.borderColor = 'var(--couleur-erreur)';
      document.getElementById('facto-q').style.borderColor = 'var(--couleur-erreur)';
      message.style.color = 'var(--couleur-erreur)';
      message.textContent = 'Il y a des erreurs. La solution était p = ' + data.solutionP + ', q = ' + data.solutionQ + '.';
    }
  } else {
    if (factoLevelIndex < factoTotalNiveaux - 1) {
      initFactorization(factoLevelIndex + 1);
    } else {
      allerVers('rsa-protocol');
    }
  }
}

async function factoGiveUp() {
  const data = await postForm('/api/factorization/giveup', {});
  document.getElementById('facto-p').value = data.solutionP;
  document.getElementById('facto-q').value = data.solutionQ;
  document.getElementById('facto-p').disabled = true;
  document.getElementById('facto-q').disabled = true;
  document.getElementById('facto-giveup').disabled = true;
  factoValidated = true;
  document.getElementById('facto-valider').textContent = 'Suite (protocole RSA)';
  document.getElementById('facto-message').style.color = 'var(--couleur-bleu)';
  document.getElementById('facto-message').textContent = 'Solution révélée (voir la fenêtre).';
  infoDialog('Solution et estimation',
    'Solution : p = ' + data.solutionP + ', q = ' + data.solutionQ + '.\n\n' +
    "Avec le General Number Field Sieve (GNFS, le meilleur algorithme\n" +
    "classique connu pour factoriser de grands nombres), un module RSA\n" +
    "réel de 2048 bits prendrait environ 6 × 10¹⁴ années-cœur de calcul :\n" +
    "même avec un cluster d'un million de cœurs dédiés exclusivement à ce\n" +
    "calcul, ça prendrait encore environ 600 millions d'années.");
}

function factoVoirEnEntier() {
  infoDialog('N en entier', factoN);
}

// =====================================================================
// RSA : PRÉSENTATION DU PROTOCOLE
// =====================================================================

let rsaDernieres = {};

async function initRsaProtocol() {
  const data = await postForm('/api/rsa/generate', {});
  rsaDernieres = data;
  document.getElementById('rsa-n').textContent = 'N = p × q = ' + formatGrand(data.n);
  document.getElementById('rsa-phin').textContent = 'φ(N) = (p−1)(q−1) = ' + formatGrand(data.phiN);
  document.getElementById('rsa-e').textContent = 'e = ' + formatGrand(data.e);
  document.getElementById('rsa-p').textContent = 'p = ' + formatGrand(data.p);
  document.getElementById('rsa-q').textContent = 'q = ' + formatGrand(data.q);
  document.getElementById('rsa-d').textContent = 'd = ' + formatGrand(data.d);
  document.getElementById('rsa-resultat-chiffrement').textContent = '';
  document.getElementById('rsa-resultat-dechiffrement').textContent = '';
}

function rsaRegenerer() {
  initRsaProtocol();
}

async function rsaChiffrer() {
  const message = document.getElementById('rsa-message').value;
  const data = await postForm('/api/rsa/encrypt', { message });
  const resultat = document.getElementById('rsa-resultat-chiffrement');
  if (!data.ok) {
    resultat.style.color = 'var(--couleur-erreur)';
    resultat.textContent = data.erreur;
    return;
  }
  resultat.style.color = '#000';
  resultat.textContent = 'm=' + formatGrand(data.m) + ' → C≡m^e(mod(N))=' + formatGrand(data.c);
  document.getElementById('rsa-resultat-dechiffrement').textContent = '';
}

async function rsaDechiffrer() {
  const data = await postForm('/api/rsa/decrypt', {});
  const resultat = document.getElementById('rsa-resultat-dechiffrement');
  if (!data.ok) {
    resultat.style.color = 'var(--couleur-erreur)';
    resultat.textContent = data.erreur;
    return;
  }
  resultat.style.color = '#000';
  resultat.textContent = 'm≡c^d(mod(N))=' + formatGrand(data.m) + ' → message déchiffré:"' + data.texte + '"';
}

function rsaVoirComplet(which) {
  if (which === 'pub') {
    infoDialog('Clé publique complète', 'N =\n' + rsaDernieres.n + '\n\nφ(N) =\n' + rsaDernieres.phiN + '\n\ne =\n' + rsaDernieres.e);
  } else {
    infoDialog('Clé privée complète', 'p =\n' + rsaDernieres.p + '\n\nq =\n' + rsaDernieres.q + '\n\nd =\n' + rsaDernieres.d);
  }
}

// =====================================================================
// KYBER : PRÉSENTATION DU PROTOCOLE
// =====================================================================

let kyberDernieres = {};

function formatPoly(arr) {
  if (arr.length <= 5) return '[' + arr.join(', ') + ']';
  return '[' + arr.slice(0, 5).join(', ') + ', ..., ' + arr[arr.length - 1] + ']';
}

async function initKyberProtocol() {
  const [cles, exemple] = await Promise.all([
    postForm('/api/kyber/generate', {}),
    getJson('/api/kyber/example'),
  ]);
  kyberDernieres = cles;

  const grilleA = document.getElementById('kyber-exemple-a');
  grilleA.innerHTML = '';
  for (let i = 0; i < 2; i++) {
    for (let j = 0; j < 2; j++) {
      const cellule = document.createElement('span');
      cellule.textContent = '[' + exemple.a[i][j].join(', ') + ']';
      grilleA.appendChild(cellule);
    }
  }
  const grilleT = document.getElementById('kyber-exemple-t');
  grilleT.innerHTML = '';
  exemple.t.forEach(poly => {
    const ligne = document.createElement('span');
    ligne.textContent = '[' + poly.join(', ') + ']';
    grilleT.appendChild(ligne);
  });
  const grilleS = document.getElementById('kyber-exemple-s');
  grilleS.innerHTML = '';
  exemple.s.forEach(poly => {
    const ligne = document.createElement('span');
    ligne.textContent = '[' + poly.join(', ') + ']';
    grilleS.appendChild(ligne);
  });

  document.getElementById('kyber-resultat-chiffrement').textContent = '';
  document.getElementById('kyber-resultat-dechiffrement').textContent = '';
}

function kyberRegenerer() {
  initKyberProtocol();
}

async function kyberChiffrer() {
  const message = document.getElementById('kyber-message').value;
  const data = await postForm('/api/kyber/encrypt', { message });
  const resultat = document.getElementById('kyber-resultat-chiffrement');
  if (!data.ok) {
    resultat.style.color = 'var(--couleur-erreur)';
    resultat.textContent = data.erreur;
    return;
  }
  resultat.style.color = '#000';
  resultat.textContent = 'u[0]=' + formatPoly(data.u0) + ' v=' + formatPoly(data.v);
  document.getElementById('kyber-resultat-dechiffrement').textContent = '';
}

async function kyberDechiffrer() {
  const data = await postForm('/api/kyber/decrypt', {});
  const resultat = document.getElementById('kyber-resultat-dechiffrement');
  if (!data.ok) {
    resultat.style.color = 'var(--couleur-erreur)';
    resultat.textContent = data.erreur;
    return;
  }
  resultat.style.color = data.succes ? '#000' : 'var(--couleur-erreur)';
  resultat.textContent = 'm=v−sᵗ·u → message déchiffré:"' + data.texte + '"' + (data.succes ? '' : ' (erreur:bits différents)');
}

function kyberVoirComplet(which) {
  if (which === 'pub') {
    let texte = 'Matrice A :\n\n';
    for (let i = 0; i < 2; i++) {
      for (let j = 0; j < 2; j++) {
        texte += 'A[' + i + '][' + j + '] = [' + kyberDernieres.a[i][j].join(', ') + ']\n\n';
      }
    }
    texte += '\nVecteur t :\n\n';
    kyberDernieres.t.forEach((poly, i) => { texte += 't[' + i + '] = [' + poly.join(', ') + ']\n\n'; });
    infoDialog('Clé publique complète', texte);
  } else {
    let texte = '';
    kyberDernieres.s.forEach((poly, i) => { texte += 's[' + i + '] = [' + poly.join(', ') + ']\n\n'; });
    infoDialog('Clé privée complète', texte);
  }
}

// =====================================================================
// RÉSEAUX EUCLIDIENS (3 exercices, code partagé)
// =====================================================================

const PORTEE_LAT = 16;

function dessinerLattice(canvas, u, v, a, b, courant) {
  const ctx = canvas.getContext('2d');
  const cote = Math.min(canvas.clientWidth, canvas.clientHeight);
  canvas.width = canvas.clientWidth;
  canvas.height = canvas.clientHeight;
  const echelle = cote / (2 * PORTEE_LAT + 2);
  const px = x => canvas.width / 2 + x * echelle;
  const py = y => canvas.height / 2 - y * echelle;

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  ctx.strokeStyle = '#eee';
  ctx.lineWidth = 1;
  for (let i = -PORTEE_LAT; i <= PORTEE_LAT; i++) {
    ctx.beginPath();
    ctx.moveTo(px(i), py(-PORTEE_LAT)); ctx.lineTo(px(i), py(PORTEE_LAT));
    ctx.moveTo(px(-PORTEE_LAT), py(i)); ctx.lineTo(px(PORTEE_LAT), py(i));
    ctx.stroke();
  }
  ctx.strokeStyle = '#ccc';
  ctx.beginPath();
  ctx.moveTo(px(-PORTEE_LAT), py(0)); ctx.lineTo(px(PORTEE_LAT), py(0));
  ctx.moveTo(px(0), py(-PORTEE_LAT)); ctx.lineTo(px(0), py(PORTEE_LAT));
  ctx.stroke();

  function vecteur(x1, y1, couleur, etiquette) {
    ctx.strokeStyle = couleur;
    ctx.fillStyle = couleur;
    ctx.lineWidth = 2.2;
    const xa = px(0), ya = py(0), xb = px(x1), yb = py(y1);
    ctx.beginPath(); ctx.moveTo(xa, ya); ctx.lineTo(xb, yb); ctx.stroke();
    const angle = Math.atan2(yb - ya, xb - xa);
    const taille = 8;
    ctx.beginPath();
    ctx.moveTo(xb, yb);
    ctx.lineTo(xb - taille * Math.cos(angle - Math.PI / 6), yb - taille * Math.sin(angle - Math.PI / 6));
    ctx.moveTo(xb, yb);
    ctx.lineTo(xb - taille * Math.cos(angle + Math.PI / 6), yb - taille * Math.sin(angle + Math.PI / 6));
    ctx.stroke();
    ctx.font = 'bold 13px sans-serif';
    ctx.fillText(etiquette, xb + 4, yb - 4);
  }

  function point(p, couleur, etiquette) {
    ctx.fillStyle = couleur;
    ctx.beginPath();
    ctx.arc(px(p[0]), py(p[1]), 5, 0, 2 * Math.PI);
    ctx.fill();
    if (etiquette) {
      ctx.font = 'bold 13px sans-serif';
      ctx.fillText(etiquette, px(p[0]) + 8, py(p[1]) - 8);
    }
  }

  vecteur(u[0], u[1], '#27ae60', 'u');
  vecteur(v[0], v[1], '#e67e22', 'v');
  // Origine = départ
  point([0, 0], '#000', '(0,0)');
  // a = cible
  if (a) point(a, '#800080', 'A');
  if (courant) point(courant, '#e74c3c', '');
}

async function initLattice(idEcran, type) {
  const conteneur = document.querySelector('#' + idEcran + ' .lattice-corps');
  const data = await getJson('/api/lattice/' + type);
  const canvas = conteneur.querySelector('.lattice-canvas');
  const spinnerA = conteneur.querySelector('.lat-spin-a');
  const spinnerB = conteneur.querySelector('.lat-spin-b');
  const section = document.getElementById(idEcran);
  const message = section.querySelector('.lat-message');
  const valider = section.querySelector('.lat-valider');

  const isCvp = type === 'cvp';

  conteneur.querySelector('.lat-u').textContent = 'u = (' + data.u[0] + ', ' + data.u[1] + ')';
  conteneur.querySelector('.lat-v').textContent = 'v = (' + data.v[0] + ', ' + data.v[1] + ')';
  const ax = isCvp ? parseFloat(data.ax) : data.a[0];
  const ay = isCvp ? parseFloat(data.ay) : data.a[1];
  const suffixeA = isCvp ? '  (cible, hors réseau)' : '  (cible)';
  conteneur.querySelector('.lat-a').textContent = 'Origine = (0, 0)  (départ)';
  conteneur.querySelector('.lat-b').textContent = 'A = (' + ax + ', ' + ay + ')' + suffixeA;

  const origine = [0, 0];

  spinnerA.value = 0;
  spinnerB.value = 0;
  spinnerA.disabled = false;
  spinnerB.disabled = false;
  message.textContent = '';
  valider.textContent = 'Valider';
  let valide = false;

  function pointCourant() {
    const a = parseInt(spinnerA.value || '0', 10);
    const b = parseInt(spinnerB.value || '0', 10);
    return [a * data.u[0] + b * data.v[0], a * data.u[1] + b * data.v[1]];
  }

  function redessiner() {
    // Pour CVP, A a des coordonnées flottantes (ax, ay)
    dessinerLattice(canvas, data.u, data.v, [ax, ay], null, pointCourant());
  }

  spinnerA.oninput = redessiner;
  spinnerB.oninput = redessiner;

  const observeur = new ResizeObserver(redessiner);
  if (canvas._observeurRedimensionnement) {
    canvas._observeurRedimensionnement.disconnect();
  }
  canvas._observeurRedimensionnement = observeur;
  observeur.observe(canvas);
  redessiner();

  valider.onclick = async () => {
    if (!valide) {
      const a = parseInt(spinnerA.value || '0', 10);
      const b = parseInt(spinnerB.value || '0', 10);
      const res = await postForm('/api/lattice/' + type, { a, b });
      valide = true;
      spinnerA.disabled = true;
      spinnerB.disabled = true;
      if (type === 'cvp') {
        valider.textContent = 'Suite (protocole Kyber)';
        if (res.correct) {
          message.style.color = 'var(--couleur-succes)';
          message.textContent = 'Bien joué ! C\'est le vecteur le plus proche (distance ' + res.distanceOptimale + ').';
        } else {
          spinnerA.value = res.solutionA;
          spinnerB.value = res.solutionB;
          redessiner();
          message.style.color = 'var(--couleur-erreur)';
          message.textContent = 'Pas optimal (distance ' + res.distanceProposee + '). Le plus proche : a=' + res.solutionA + ', b=' + res.solutionB + ' (distance ' + res.distanceOptimale + ').';
        }
      } else {
        valider.textContent = 'Suite';
        if (res.correct) {
          message.style.color = 'var(--couleur-succes)';
          message.textContent = 'Bien joué !';
        } else {
          spinnerA.value = res.solutionA;
          spinnerB.value = res.solutionB;
          redessiner();
          message.style.color = 'var(--couleur-erreur)';
          message.textContent = 'Il y a des erreurs. La solution était a = ' + res.solutionA + ', b = ' + res.solutionB + '.';
        }
      }
    } else {
      if (type === 'standard') allerVers('lattice-combination');
      else if (type === 'combination') allerVers('lattice-cvp');
      else allerVers('kyber-protocol');
    }
  };
}
