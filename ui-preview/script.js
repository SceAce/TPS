const phone = document.querySelector(".phone");
const modeButtons = document.querySelectorAll(".control");
const tabs = document.querySelectorAll(".tab");
const screens = document.querySelectorAll(".screen");
const refreshButton = document.querySelector(".refresh-dot");
const navCards = document.querySelectorAll(".nav-card, .jump");
const keyboardInputs = document.querySelectorAll(".chat-input input, .publish-screen input, .publish-screen textarea");

function showScreen(targetId) {
  screens.forEach((screen) => {
    screen.classList.toggle("active", screen.id === targetId);
  });
  phone.dataset.screen = targetId;
  tabs.forEach((item) => {
    item.classList.toggle("active", item.dataset.target === targetId);
  });
  setMode("idle");
}

function setMode(mode) {
  phone.dataset.mode = mode;
  modeButtons.forEach((button) => {
    button.classList.toggle("active", button.dataset.mode === mode);
  });
}

modeButtons.forEach((button) => {
  button.addEventListener("click", () => setMode(button.dataset.mode));
});

tabs.forEach((tab) => {
  tab.addEventListener("click", () => {
    showScreen(tab.dataset.target);
  });
});

navCards.forEach((card) => {
  card.addEventListener("click", () => {
    showScreen(card.dataset.target);
  });
});

refreshButton.addEventListener("click", () => {
  setMode("refreshing");
  window.setTimeout(() => setMode("idle"), 1800);
});

keyboardInputs.forEach((input) => {
  input.addEventListener("focus", () => setMode("keyboard"));
});

let startY = 0;
let pulling = false;
const home = document.querySelector("#screen-home");

home.addEventListener("pointerdown", (event) => {
  if (home.scrollTop === 0) {
    startY = event.clientY;
    pulling = true;
  }
});

home.addEventListener("pointermove", (event) => {
  if (!pulling) return;
  const distance = event.clientY - startY;
  if (distance > 24) setMode("pulling");
});

home.addEventListener("pointerup", (event) => {
  if (!pulling) return;
  const distance = event.clientY - startY;
  pulling = false;
  if (distance > 70) {
    setMode("refreshing");
    window.setTimeout(() => setMode("idle"), 1800);
  } else {
    setMode("idle");
  }
});

setMode("idle");
phone.dataset.screen = "screen-home";
