(function () {
  'use strict';

  // ---- Theme toggle ----
  var root = document.documentElement;
  var toggle = document.getElementById('themeToggle');
  function syncIcon() {
    if (!toggle) return;
    var dark = root.getAttribute('data-bs-theme') === 'dark';
    toggle.innerHTML = dark ? '<i class="bi bi-sun"></i>' : '<i class="bi bi-moon-stars"></i>';
  }
  if (toggle) {
    toggle.addEventListener('click', function () {
      var next = root.getAttribute('data-bs-theme') === 'dark' ? 'light' : 'dark';
      root.setAttribute('data-bs-theme', next);
      try { localStorage.setItem('theme', next); } catch (e) { /* private mode */ }
      syncIcon();
    });
  }
  syncIcon();

  // ---- Code highlighting ----
  if (window.hljs) { window.hljs.highlightAll(); }

  // ---- Interview page: auto-submit filters and client-side search ----
  document.querySelectorAll('[data-autosubmit]').forEach(function (el) {
    el.addEventListener('change', function () { el.form.submit(); });
  });
  var filter = document.getElementById('interviewFilter');
  if (filter) {
    filter.addEventListener('input', function () {
      var q = filter.value.trim().toLowerCase();
      document.querySelectorAll('.interview-item').forEach(function (item) {
        item.classList.toggle('d-none', q !== '' && item.textContent.toLowerCase().indexOf(q) === -1);
      });
    });
  }

  // ---- Quiz ----
  var form = document.getElementById('quizForm');
  if (!form) return;

  var csrfToken = (document.querySelector('meta[name="_csrf"]') || {}).content;
  var csrfHeader = (document.querySelector('meta[name="_csrf_header"]') || {}).content;
  var resultEl = document.getElementById('quizResult');

  form.addEventListener('submit', async function (e) {
    e.preventDefault();

    var answers = {};
    form.querySelectorAll('.q').forEach(function (q) {
      var checked = q.querySelector('input:checked');
      if (checked) answers[q.dataset.id] = parseInt(checked.value, 10);
    });

    var headers = { 'Content-Type': 'application/json' };
    if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken;

    try {
      var res = await fetch(form.dataset.url, {
        method: 'POST', headers: headers, body: JSON.stringify(answers)
      });
      if (!res.ok) throw new Error('HTTP ' + res.status);
      var data = await res.json();

      data.review.forEach(function (r) {
        var box = form.querySelector('.q[data-id="' + r.questionId + '"]');
        if (!box) return;
        var fb = box.querySelector('.q-feedback');
        fb.className = 'q-feedback small mt-2 ' + (r.correct ? 'ok' : 'bad');
        var label = box.querySelectorAll('.form-check-label')[r.correctIdx];
        fb.textContent = (r.correct ? 'Correct. ' : 'Not quite. The right answer is "' +
          (label ? label.textContent : '') + '". ') + (r.explanation || '');
      });

      resultEl.textContent = 'Score: ' + data.score + ' / ' + data.total;
    } catch (err) {
      resultEl.textContent = 'Could not check answers. Please try again.';
      console.error(err);
    }
  });
})();
