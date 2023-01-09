/*
 * PDF プレビューサンプル
 *
 * [require]: libreoffice インストール
 * [back]   : PDF を作成
 * [back]   : ファイル URL/ファイル名を送信する
 * [front]  : PDF.js でプレビュー表示
 * [front]  : プレビュー閉じたら PDF ファイルを削除
 */

var pdfDoc = null;
var pageNum = 1;
var pageRendering = false;
var pageNumPending = null;
var scale = 1.0;
var canvas = document.getElementById("canvas-preview");
var context = canvas.getContext("2d");

document.addEventListener('DOMContentLoaded', () => {
  document
    .querySelectorAll('button[name="show-preview"]')
    .forEach((e) => {
      e.addEventListener("click", () => {
        document.getElementById("canvas-control__text-indicator").textContent = "rendering...";
        let url = e.dataset.url;
        requestMakePDF(url);
      });
    });

  document.getElementById("canvas-control__button-next-page").addEventListener("click", onNextPage);
  document.getElementById("canvas-control__button-previous-page").addEventListener("click", onPreviousPage);
});

async function requestMakePDF(url) {
  const response = await fetch(url, { method: "GET" });
  const data = await response.json();
  if (data.result == "ok") {
    console.info("Convert success.");
    showPreview(data.url);
  } else {
    console.warn("Convert failed.");
  }
}

function renderPage(num) {
  pageRendering = true;

  pdfDoc.getPage(num).then((page) => {
    let viewport = page.getViewport({
      scale: scale
    });
    canvas.height = viewport.height;
    canvas.width = viewport.width;

    let renderContext = {
      canvasContext: context,
      viewport: viewport
    };
    let renderTask = page.render(renderContext);

    renderTask.promise.then(() => {
      pageRendering = false;
      if (pageNumPending !== null) {
        renderPage(pageNumPending);
        pageNumPending = null;
      }
    });
  });

  document.getElementById("canvas-control__text-page-num").textContent = num;
  document.getElementById("canvas-control__text-indicator").textContent = "ready";
}

function queueRenderPage(num) {
  console.debug("queueRenderPage");
  if (pageRendering) {
    pageNumPending = num;
  } else {
    renderPage(num);
  }
}

function onPreviousPage() {
  console.debug("onPreviousPage");
  if (pageNum <= 1) {
    return;
  }

  pageNum--;
  queueRenderPage(pageNum);
}

function onNextPage() {
  console.debug("onNextPage");
  if (pdfDoc.numPages <= pageNum) {
    return;
  }

  pageNum++;
  queueRenderPage(pageNum);
}

function showPreview(fileUrl) {
  let pdfJsLib = window["pdfjs-dist/build/pdf"];
  pdfJsLib.GlobalWorkerOptions.workerSrc = '//mozilla.github.io/pdf.js/build/pdf.worker.js';

  pdfJsLib.getDocument(fileUrl).promise.then((newPdfDoc) => {
    pdfDoc = newPdfDoc;
    document.getElementById("canvas-control__text-page-count").textContent = pdfDoc.numPages;

    renderPage(pageNum);
  });
};
