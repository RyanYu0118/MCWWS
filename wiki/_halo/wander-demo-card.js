(function () {
  const card = document.getElementById("wanderCard");
  if (!card) return;
  const video = card.querySelector("video");
  const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
    navigator.userAgent
  );
  function updateMousePosition(e) {
    const rect = card.getBoundingClientRect();
    card.style.setProperty("--x", e.clientX - rect.left + "px");
    card.style.setProperty("--y", e.clientY - rect.top + "px");
  }
  if (isMobile) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && entry.intersectionRatio >= 0.95) {
            card.style.setProperty("--x", "50%");
            card.style.setProperty("--y", "50%");
            if (video && video.src) {
              video.play().then(() => card.classList.add("is-playing")).catch(() => {});
            } else {
              card.classList.add("is-playing");
            }
          } else {
            if (video) video.pause();
            card.classList.remove("is-playing");
          }
        });
      },
      { threshold: [0.95] }
    );
    observer.observe(card);
  } else {
    card.addEventListener("mouseenter", (e) => {
      updateMousePosition(e);
      if (video && video.src) {
        video.play().then(() => card.classList.add("is-playing")).catch(() => {});
      } else {
        card.classList.add("is-playing");
      }
    });
    card.addEventListener("mousemove", updateMousePosition);
    card.addEventListener("mouseleave", () => {
      card.style.setProperty("--x", "50%");
      card.style.setProperty("--y", "50%");
      if (video) video.pause();
      card.classList.remove("is-playing");
    });
  }
})();
