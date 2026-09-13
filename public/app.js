'use strict';

let chapters = [];
let sortKey = null;
let ascending = true;
const tbody = document.querySelector('#chapters');
const status = document.querySelector('#status');

function render() {
  tbody.replaceChildren();
  chapters.forEach(chapter => {
    const row = document.createElement('tr');
    const number = document.createElement('th');
    number.scope = 'row';
    number.className = 'chapter-number';
    number.textContent = `第${chapter.chapter}章`;
    row.append(number);

    const content = document.createElement('td');
    content.className = 'chapter-content';
    const title = document.createElement('strong');
    title.textContent = chapter.title;
    const topics = document.createElement('ul');
    for (const topic of chapter.topics) {
      const item = document.createElement('li');
      item.textContent = topic;
      topics.append(item);
    }
    content.append(title, topics);
    row.append(content);

    const cell = document.createElement('td');
    cell.className = 'understanding-cell';
    const input = document.createElement('input');
    input.type = 'number';
    input.setAttribute('aria-label', `第${chapter.chapter}章の理解度`);
    input.setAttribute('aria-describedby', 'understanding-help');
    input.value = chapter.understanding ?? '';
    input.placeholder = '未入力';
    input.min = '0';
    input.max = '100';
    input.step = 'any';
    input.inputMode = 'decimal';
    const progress = document.createElement('progress');
    progress.max = 100;
    progress.value = chapter.understanding ?? 0;
    progress.hidden = chapter.understanding === null;
    progress.setAttribute('aria-label', `第${chapter.chapter}章の理解度`);
    input.addEventListener('change', () => {
      const raw = input.value.trim();
      const value = raw === '' ? null : Number(raw);
      if (input.validity.badInput || (value !== null && (!Number.isFinite(value) || value < 0 || value > 100))) {
        status.textContent = '理解度は0〜100の数値で入力してください。変更を取り消しました。';
        input.value = chapter.understanding ?? '';
        return;
      }
      chapter.understanding = value;
      progress.value = value ?? 0;
      progress.hidden = value === null;
      sortKey = null;
      updateSortLabels();
      updateCount();
      status.textContent = `第${chapter.chapter}章の理解度を${value === null ? '未入力に戻しました' : value + '％に変更しました'}（この画面のみ・保存なし）。`;
    });
    input.addEventListener('keydown', event => {
      if (event.key === 'Enter') input.blur();
      if (event.key === 'Escape') {
        input.value = chapter.understanding ?? '';
        input.blur();
      }
    });
    cell.append(input, progress);
    row.append(cell);
    tbody.append(row);
  });
  updateCount();
}

function updateCount() {
  const entered = chapters.filter(chapter => chapter.understanding !== null).length;
  document.querySelector('#count').textContent = `全${chapters.length}章 · 理解度入力済み ${entered}章`;
}

function updateSortLabels() {
  for (const key of ['chapter', 'understanding']) {
    const button = document.querySelector(`#sort-${key}`);
    button.parentElement.setAttribute('aria-sort', sortKey === key
      ? (ascending ? 'ascending' : 'descending') : 'none');
    button.querySelector('span').textContent = sortKey === key ? (ascending ? '↑' : '↓') : '↕';
  }
}

for (const key of ['chapter', 'understanding']) {
  document.querySelector(`#sort-${key}`).addEventListener('click', () => {
    ascending = sortKey === key ? !ascending : true;
    sortKey = key;
    chapters.sort((a, b) => {
      // 未入力は昇順・降順とも末尾へ。0％とは区別する。
      if (key === 'understanding') {
        if (a[key] === null && b[key] !== null) return 1;
        if (a[key] !== null && b[key] === null) return -1;
      }
      return (a[key] - b[key]) * (ascending ? 1 : -1) || a.chapter - b.chapter;
    });
    render();
    updateSortLabels();
    status.textContent = `${key === 'understanding' ? '理解度' : '章番号'}の${ascending ? '昇順' : '降順'}に並べ替えました。`;
  });
}

async function load() {
  const retry = document.querySelector('#retry');
  retry.hidden = true;
  status.textContent = '学習内容を読み込んでいます。';
  try {
    const response = await fetch('/api/chapters');
    if (!response.ok) throw new Error('Failed to load chapters');
    chapters = await response.json();
    sortKey = null;
    render();
    updateSortLabels();
    status.textContent = '理解度を入力したらEnterキー、または別のセルを選んで確定します。';
  } catch (error) {
    document.querySelector('#count').textContent = '読み込みエラー';
    status.textContent = '学習内容を読み込めませんでした。時間をおいて再読み込みしてください。';
    retry.hidden = false;
  }
}
document.querySelector('#retry').addEventListener('click', load);
load();
