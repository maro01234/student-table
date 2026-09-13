'use strict';

let students = [];
let sortKey = null;
let ascending = true;
const tbody = document.querySelector('#students');
const status = document.querySelector('#status');

function render() {
  tbody.replaceChildren();
  students.forEach((student, index) => {
    const row = document.createElement('tr');
    const number = document.createElement('th');
    number.scope = 'row';
    number.className = 'row-number';
    number.textContent = index + 1;
    row.append(number);
    for (const key of ['name', 'score']) {
      const cell = document.createElement('td');
      const input = document.createElement('input');
      input.type = key === 'score' ? 'number' : 'text';
      input.setAttribute('aria-label', `${index + 1}行目の${key === 'score' ? '平均点' : '生徒名'}`);
      input.value = key === 'score' ? student.score.toFixed(1) : student.name;
      if (key === 'score') {
        input.min = '0';
        input.max = '100';
        input.step = 'any';
        input.inputMode = 'decimal';
      } else {
        input.maxLength = 100;
      }
      input.required = true;
      input.addEventListener('change', () => {
        const raw = input.value.trim();
        const score = Number(raw);
        if (!raw || (key === 'score' && (!Number.isFinite(score) || score < 0 || score > 100))) {
          status.textContent = key === 'score' ? '平均点は0〜100の数値で入力してください。変更を取り消しました。'
            : '生徒名を入力してください。変更を取り消しました。';
          input.value = key === 'score' ? student.score.toFixed(1) : student.name;
          return;
        }
        student[key] = key === 'score' ? score : raw;
        input.value = key === 'score' ? score.toFixed(1) : raw;
        // 編集後は表示順を維持し、ソート済みという表示を解除する。
        sortKey = null;
        updateSortLabels();
        status.textContent = '変更を反映しました（この画面のみ・保存なし）。';
      });
      input.addEventListener('keydown', event => {
        if (event.key === 'Enter') input.blur();
        if (event.key === 'Escape') {
          input.value = key === 'score' ? student.score.toFixed(1) : student.name;
          input.blur();
        }
      });
      cell.append(input);
      row.append(cell);
    }
    tbody.append(row);
  });
  document.querySelector('#count').textContent = `${students.length}名の生徒`;
}

function updateSortLabels() {
  for (const key of ['name', 'score']) {
    const button = document.querySelector(`#sort-${key}`);
    button.parentElement.setAttribute('aria-sort', sortKey === key
      ? (ascending ? 'ascending' : 'descending') : 'none');
    button.querySelector('span').textContent = sortKey === key ? (ascending ? '↑' : '↓') : '↕';
  }
}

for (const key of ['name', 'score']) {
  document.querySelector(`#sort-${key}`).addEventListener('click', () => {
    ascending = sortKey === key ? !ascending : true;
    sortKey = key;
    students.sort((a, b) => (key === 'score' ? a.score - b.score
      : a.name.localeCompare(b.name, 'ja')) * (ascending ? 1 : -1));
    render();
    updateSortLabels();
    status.textContent = `${key === 'score' ? '平均点' : '生徒名'}の${ascending ? '昇順' : '降順'}に並べ替えました。`;
  });
}

async function load() {
  const retry = document.querySelector('#retry');
  retry.hidden = true;
  status.textContent = 'データを読み込んでいます。';
  try {
    const response = await fetch('/api/students');
    if (!response.ok) throw new Error('Failed to load students');
    students = await response.json();
    sortKey = null;
    render();
    updateSortLabels();
    status.textContent = 'セルを編集したらEnterキー、または別のセルを選んで確定します。';
  } catch (error) {
    document.querySelector('#count').textContent = '読み込みエラー';
    status.textContent = 'データを読み込めませんでした。時間をおいて再読み込みしてください。';
    retry.hidden = false;
  }
}
document.querySelector('#retry').addEventListener('click', load);
load();
