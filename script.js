document.addEventListener("DOMContentLoaded", function () {
    const body = document.body;
    const mobileMenu = document.getElementById("mobile-menu");
    const navList = document.getElementById("nav-list");
    const navLinks = document.querySelectorAll("nav a");
    const navDisclosureButtons = document.querySelectorAll(".nav-disclosure");
    const header = document.querySelector("header");
    const backToHomeButton = document.getElementById("backToHomeButton");
    const gallerySection = document.getElementById("gallery");
    const modal = document.getElementById("myModal");
    const modalPlaceholder = document.getElementById("modalPlaceholder");
    const modalHires = document.getElementById("modalHires");
    const modalImageContainer = document.querySelector(".modal-image-container");
    const downloadBtn = document.getElementById("downloadBtn");
    const closeBtn = document.querySelector(".close");
    const imageCards = document.querySelectorAll(".image-card");
    const carouselImages = document.querySelectorAll(".carousel-image");
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");
    const form = document.querySelector("#contact form");
    const feedbackBlockquote = document.querySelector("#feedback blockquote");
    const internalHashLinks = document.querySelectorAll('a[href^="#"]');
    const defaultApiBase = window.location.protocol === "file:" ? "http://localhost:8080" : "";

    const brandHashes = new Set([
        "#brands",
        "#benz",
        "#porsche",
        "#hongqi",
        "#xiaomi",
        "#bmw",
        "#audi",
        "#ferrari",
        "#lamborghini",
        "#astonmartin",
        "#maserati",
        "#bugatti",
        "#ford"
    ]);
    const contactHashes = new Set(["#contact", "#feedback"]);
    const scrollLocks = new Set();
    const preloadCache = Object.create(null);

    let modalScale = 1;
    let carouselIndex = 0;
    let carouselTimer = null;
    let userStoppedCarousel = false;

    function isMobileViewport() {
        return window.matchMedia("(max-width: 768px)").matches;
    }

    function addScrollLock(key) {
        scrollLocks.add(key);
        syncScrollLock();
    }

    function removeScrollLock(key) {
        scrollLocks.delete(key);
        syncScrollLock();
    }

    function syncScrollLock() {
        const shouldLock = scrollLocks.size > 0;

        body.classList.toggle("scroll-locked", shouldLock);

        body.classList.toggle("menu-open", scrollLocks.has("menu"));
        body.classList.toggle("modal-open", scrollLocks.has("modal"));
        updateBackToHomeButton();
    }

    function collapseDisclosure(button) {
        const parent = button.closest(".has-children");
        if (!parent) return;

        parent.classList.remove("is-open");
        button.setAttribute("aria-expanded", "false");
    }

    function collapseAllDisclosures(exceptButton) {
        navDisclosureButtons.forEach(function (button) {
            if (button !== exceptButton) {
                collapseDisclosure(button);
            }
        });
    }

    function closeMenu() {
        if (!mobileMenu || !navList) return;

        navList.classList.remove("active");
        mobileMenu.setAttribute("aria-expanded", "false");
        collapseAllDisclosures();
        removeScrollLock("menu");
    }

    function openMenu() {
        if (!mobileMenu || !navList) return;

        navList.classList.add("active");
        mobileMenu.setAttribute("aria-expanded", "true");
        if (isMobileViewport()) {
            addScrollLock("menu");
        }
    }

    function toggleMenu() {
        if (!mobileMenu || !navList) return;

        if (navList.classList.contains("active")) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    function highlightCurrentNav() {
        const currentHash = window.location.hash;
        const currentFile = (window.location.pathname.split("/").pop() || "").toLowerCase();
        const fallbackHash = gallerySection ? "#gallery" : "";
        const activeHash = currentHash || fallbackHash;

        navLinks.forEach(function (link) {
            const href = link.getAttribute("href") || "";
            let isActive = false;

            if (href.startsWith("#")) {
                isActive = href === activeHash;

                if (!isActive && href === "#contact" && contactHashes.has(activeHash)) {
                    isActive = true;
                }

                if (!isActive && href === "#brands" && brandHashes.has(activeHash)) {
                    isActive = true;
                }
            } else if (currentFile) {
                isActive = currentFile === href.toLowerCase();
            }

            link.classList.toggle("active", isActive);
        });
    }

    function updateBackToHomeButton() {
        if (!backToHomeButton) return;

        const href = backToHomeButton.getAttribute("href") || "";
        const isMainPageButton = href === "#gallery";
        const currentHash = window.location.hash;
        const shouldShow = !isMainPageButton || window.scrollY > 260 || (currentHash && currentHash !== "#gallery");

        backToHomeButton.classList.toggle(
            "show",
            shouldShow && !scrollLocks.has("menu") && !scrollLocks.has("modal")
        );
    }

    function getHeaderOffset() {
        if (isMobileViewport() && header) {
            return header.getBoundingClientRect().height + 12;
        }

        return 24;
    }

    function scrollToHashTarget(hash, shouldUpdateHash) {
        if (!hash || hash === "#") return false;

        const target = document.querySelector(hash);
        if (!target) return false;

        const targetTop = target.getBoundingClientRect().top + window.scrollY - getHeaderOffset();
        window.scrollTo({
            top: Math.max(targetTop, 0),
            left: 0,
            behavior: "auto"
        });

        if (shouldUpdateHash) {
            history.replaceState(null, "", hash);
        }

        highlightCurrentNav();
        updateBackToHomeButton();
        return true;
    }

    function getPreviewSrc(card) {
        const image = card.querySelector("img");
        if (!image) return "";
        return image.currentSrc || image.getAttribute("src") || "";
    }

    function getHiResPreviewSrc(fullSrc) {
        if (!fullSrc) return "";
        return fullSrc.replace(/\.(jpe?g|png)$/i, ".webp");
    }

    function getActiveModalImage() {
        if (modalHires && modalHires.classList.contains("loaded") && modalHires.naturalWidth && modalHires.naturalHeight) {
            return {
                element: modalHires,
                scale: modalScale
            };
        }

        if (modalPlaceholder && modalPlaceholder.naturalWidth && modalPlaceholder.naturalHeight) {
            return {
                element: modalPlaceholder,
                scale: 1.05
            };
        }

        if (modalHires && modalHires.naturalWidth && modalHires.naturalHeight) {
            return {
                element: modalHires,
                scale: modalScale
            };
        }

        return null;
    }

    function isPointerInsideModalImage(event) {
        if (!modalImageContainer) {
            return false;
        }

        const activeImage = getActiveModalImage();
        if (!activeImage) {
            return true;
        }

        const containerRect = modalImageContainer.getBoundingClientRect();
        const naturalWidth = activeImage.element.naturalWidth;
        const naturalHeight = activeImage.element.naturalHeight;

        if (!containerRect.width || !containerRect.height || !naturalWidth || !naturalHeight) {
            return true;
        }

        const containerRatio = containerRect.width / containerRect.height;
        const imageRatio = naturalWidth / naturalHeight;
        let renderedWidth = containerRect.width;
        let renderedHeight = containerRect.height;

        if (imageRatio > containerRatio) {
            renderedHeight = containerRect.width / imageRatio;
        } else {
            renderedWidth = containerRect.height * imageRatio;
        }

        renderedWidth *= activeImage.scale;
        renderedHeight *= activeImage.scale;

        const left = containerRect.left + (containerRect.width - renderedWidth) / 2;
        const top = containerRect.top + (containerRect.height - renderedHeight) / 2;
        const right = left + renderedWidth;
        const bottom = top + renderedHeight;

        return event.clientX >= left && event.clientX <= right && event.clientY >= top && event.clientY <= bottom;
    }

    function preloadHiRes(card) {
        const fullSrc = card.dataset.full || "";
        const hiResSrc = getHiResPreviewSrc(fullSrc) || fullSrc;

        if (!hiResSrc || preloadCache[hiResSrc]) {
            return;
        }

        const image = new Image();
        image.src = hiResSrc;
        preloadCache[hiResSrc] = image;
    }

    function closeModal() {
        if (!modal || !modalPlaceholder || !modalHires || !downloadBtn) return;

        modal.classList.remove("show");
        modal.setAttribute("aria-hidden", "true");
        modalHires.classList.remove("loaded");
        modalHires.src = "";
        modalPlaceholder.src = "";
        modalScale = 1;
        modalHires.style.transform = "scale(1)";
        downloadBtn.style.display = "none";
        downloadBtn.href = "#";
        removeScrollLock("modal");
    }

    function openModal(card) {
        if (!modal || !modalPlaceholder || !modalHires || !downloadBtn) return;

        const fullSrc = card.dataset.full || "";
        const hiResSrc = getHiResPreviewSrc(fullSrc) || fullSrc;
        const previewSrc = getPreviewSrc(card);

        modal.classList.add("show");
        modal.setAttribute("aria-hidden", "false");
        addScrollLock("modal");

        modalScale = 1;
        modalHires.style.transform = "scale(1)";
        modalHires.classList.remove("loaded");
        modalHires.src = "";

        modalPlaceholder.src = previewSrc;
        downloadBtn.href = fullSrc;
        downloadBtn.setAttribute("download", fullSrc.split("/").pop() || "wallpaper");
        downloadBtn.style.display = "inline-flex";

        const cachedImage = preloadCache[hiResSrc];

        function showHiRes() {
            modalHires.src = hiResSrc;
            requestAnimationFrame(function () {
                requestAnimationFrame(function () {
                    modalHires.classList.add("loaded");
                });
            });
        }

        if (cachedImage && cachedImage.complete) {
            showHiRes();
            return;
        }

        const hiResImage = cachedImage || new Image();
        hiResImage.onload = showHiRes;
        if (!cachedImage) {
            hiResImage.src = hiResSrc;
            preloadCache[hiResSrc] = hiResImage;
        }
    }

    function showCarouselImage(index) {
        carouselImages.forEach(function (image, imageIndex) {
            const isActive = imageIndex === index;
            image.style.opacity = isActive ? "1" : "0";
            image.classList.toggle("active", isActive);
            image.setAttribute("aria-hidden", isActive ? "false" : "true");
        });
    }

    function showNextCarouselImage() {
        carouselIndex = (carouselIndex + 1) % carouselImages.length;
        showCarouselImage(carouselIndex);
    }

    function showPreviousCarouselImage() {
        carouselIndex = (carouselIndex - 1 + carouselImages.length) % carouselImages.length;
        showCarouselImage(carouselIndex);
    }

    function stopCarousel() {
        if (carouselTimer) {
            clearInterval(carouselTimer);
            carouselTimer = null;
        }
    }

    function startCarousel() {
        if (carouselImages.length === 0) return;
        if (userStoppedCarousel && isMobileViewport()) return;

        stopCarousel();
        carouselTimer = setInterval(showNextCarouselImage, 5000);
    }

    function handleManualCarousel(direction) {
        if (direction === "next") {
            showNextCarouselImage();
        } else {
            showPreviousCarouselImage();
        }

        if (isMobileViewport()) {
            userStoppedCarousel = true;
            stopCarousel();
        }
    }

    function setFieldState(field, message) {
        if (!field) return;

        const hasError = Boolean(message);
        field.element.setAttribute("aria-invalid", hasError ? "true" : "false");
        field.error.textContent = message || "";
    }

    function validateField(field) {
        const value = field.element.value.trim();

        if (field.element.id === "name") {
            if (!value) {
                setFieldState(field, "请输入姓名。");
                return false;
            }
        }

        if (field.element.id === "email") {
            if (!value) {
                setFieldState(field, "请输入邮箱地址。");
                return false;
            }

            const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailPattern.test(value)) {
                setFieldState(field, "请输入有效的邮箱地址。");
                return false;
            }
        }

        if (field.element.id === "message") {
            if (!value) {
                setFieldState(field, "请输入留言内容。");
                return false;
            }
        }

        setFieldState(field, "");
        return true;
    }

    function ensureFormStatusElement(formElement) {
        if (!formElement) return null;

        let statusElement = formElement.querySelector(".form-status");
        if (statusElement) {
            return statusElement;
        }

        statusElement = document.createElement("p");
        statusElement.className = "form-status";
        statusElement.setAttribute("role", "status");
        statusElement.setAttribute("aria-live", "polite");
        formElement.appendChild(statusElement);
        return statusElement;
    }

    function setFormStatus(statusElement, message, type) {
        if (!statusElement) return;

        statusElement.textContent = message || "";
        statusElement.classList.remove("is-success", "is-error");

        if (type === "success") {
            statusElement.classList.add("is-success");
        }

        if (type === "error") {
            statusElement.classList.add("is-error");
        }
    }

    function getApiBase() {
        const configuredApiBase = body.getAttribute("data-api-base") || "";
        if (configuredApiBase) {
            return configuredApiBase.replace(/\/$/, "");
        }

        return defaultApiBase;
    }

    function buildApiUrl(path) {
        return `${getApiBase()}${path}`;
    }

    function applyServerFieldErrors(fields, fieldErrors) {
        if (!fieldErrors) {
            return;
        }

        fields.forEach(function (field) {
            const serverMessage = fieldErrors[field.element.id];
            if (serverMessage) {
                setFieldState(field, serverMessage);
            }
        });
    }

    function renderFeedbackHighlights(items) {
        if (!feedbackBlockquote || !Array.isArray(items) || items.length === 0) {
            return;
        }

        feedbackBlockquote.innerHTML = "";

        items.slice(0, 2).forEach(function (item) {
            const paragraph = document.createElement("p");
            paragraph.textContent = `"${item.message}" - ${item.name}`;
            feedbackBlockquote.appendChild(paragraph);
        });
    }

    function loadFeedbackHighlights() {
        if (!feedbackBlockquote) {
            return;
        }

        fetch(buildApiUrl("/api/feedback/highlights"), {
            headers: {
                Accept: "application/json"
            }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Failed to load feedback highlights");
                }

                return response.json();
            })
            .then(renderFeedbackHighlights)
            .catch(function () {
                // Keep the static fallback quotes when the backend is unavailable.
            });
    }

    if (mobileMenu && navList) {
        mobileMenu.addEventListener("click", toggleMenu);
    }

    navDisclosureButtons.forEach(function (button) {
        button.addEventListener("click", function (event) {
            event.preventDefault();
            event.stopPropagation();

            if (!isMobileViewport()) {
                return;
            }

            const parent = button.closest(".has-children");
            if (!parent) {
                return;
            }

            const isOpen = parent.classList.contains("is-open");
            collapseAllDisclosures(button);

            if (isOpen) {
                collapseDisclosure(button);
                return;
            }

            parent.classList.add("is-open");
            button.setAttribute("aria-expanded", "true");
        });
    });

    navLinks.forEach(function (link) {
        link.addEventListener("click", function (event) {
            if (!isMobileViewport()) {
                return;
            }

            const href = link.getAttribute("href") || "";

            if (href.startsWith("#")) {
                event.preventDefault();
                closeMenu();

                window.setTimeout(function () {
                    scrollToHashTarget(href, true);
                }, 60);
                return;
            }

            closeMenu();
        });
    });

    internalHashLinks.forEach(function (link) {
        if (link.closest("nav")) {
            return;
        }

        link.addEventListener("click", function (event) {
            const href = link.getAttribute("href") || "";
            if (!href.startsWith("#")) {
                return;
            }

            event.preventDefault();
            scrollToHashTarget(href, true);
        });
    });

    document.addEventListener("click", function (event) {
        if (!isMobileViewport() || !navList || !mobileMenu) {
            return;
        }

        const clickedInsideNav = event.target.closest("nav");
        if (!clickedInsideNav && navList.classList.contains("active")) {
            closeMenu();
        }
    });

    if (modal && modalPlaceholder && modalHires && downloadBtn) {
        modal.setAttribute("aria-hidden", "true");

        imageCards.forEach(function (card) {
            card.addEventListener("mouseenter", function () {
                preloadHiRes(card);
            });

            card.addEventListener("touchstart", function () {
                preloadHiRes(card);
            }, { passive: true });

            card.addEventListener("click", function () {
                openModal(card);
            });
        });

        if (closeBtn) {
            closeBtn.addEventListener("click", closeModal);
        }

        modal.addEventListener("click", function (event) {
            if (!modal.classList.contains("show")) {
                return;
            }

            if (event.target.closest(".close") || event.target.closest(".modal-actions")) {
                return;
            }

            const clickedInsideImageContainer = event.target.closest(".modal-image-container");
            if (!clickedInsideImageContainer) {
                closeModal();
                return;
            }

            if (!isPointerInsideModalImage(event)) {
                closeModal();
            }
        });

        modalHires.addEventListener("wheel", function (event) {
            event.preventDefault();

            if (event.deltaY < 0) {
                modalScale += 0.1;
            } else if (modalScale > 0.2) {
                modalScale -= 0.1;
            }

            modalHires.style.transform = `scale(${modalScale})`;
        });

        window.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && modal.classList.contains("show")) {
                closeModal();
            }
        });
    }

    if (carouselImages.length > 0) {
        showCarouselImage(carouselIndex);
        startCarousel();

        document.addEventListener("visibilitychange", function () {
            if (document.hidden) {
                stopCarousel();
            } else {
                startCarousel();
            }
        });

        if (nextBtn) {
            nextBtn.addEventListener("click", function () {
                handleManualCarousel("next");
            });
        }

        if (prevBtn) {
            prevBtn.addEventListener("click", function () {
                handleManualCarousel("prev");
            });
        }
    }

    if (form) {
        const submitButton = form.querySelector(".form-submit");
        const formStatus = ensureFormStatusElement(form);
        const fields = [
            {
                element: document.getElementById("name"),
                error: document.getElementById("nameError")
            },
            {
                element: document.getElementById("email"),
                error: document.getElementById("emailError")
            },
            {
                element: document.getElementById("message"),
                error: document.getElementById("messageError")
            }
        ].filter(function (field) {
            return field.element && field.error;
        });

        fields.forEach(function (field) {
            field.element.addEventListener("input", function () {
                validateField(field);
            });

            field.element.addEventListener("blur", function () {
                validateField(field);
            });
        });

        form.addEventListener("submit", async function (event) {
            let firstInvalidField = null;

            fields.forEach(function (field) {
                const isValid = validateField(field);
                if (!isValid && !firstInvalidField) {
                    firstInvalidField = field.element;
                }
            });

            if (firstInvalidField) {
                event.preventDefault();
                firstInvalidField.focus();
                return;
            }

            event.preventDefault();
            setFormStatus(formStatus, "", "");

            if (submitButton) {
                submitButton.disabled = true;
                submitButton.setAttribute("aria-busy", "true");
            }

            try {
                const response = await fetch(buildApiUrl("/api/feedback"), {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Accept: "application/json"
                    },
                    body: JSON.stringify({
                        name: document.getElementById("name").value.trim(),
                        email: document.getElementById("email").value.trim(),
                        message: document.getElementById("message").value.trim(),
                        page: window.location.pathname || "/main.html"
                    })
                });

                const result = await response.json().catch(function () {
                    return {};
                });

                if (!response.ok) {
                    applyServerFieldErrors(fields, result.fieldErrors);
                    setFormStatus(
                        formStatus,
                        result.message || "提交失败，请稍后再试。",
                        "error"
                    );
                    return;
                }

                form.reset();
                fields.forEach(function (field) {
                    setFieldState(field, "");
                });
                setFormStatus(
                    formStatus,
                    result.message || "反馈已提交，感谢你的建议。",
                    "success"
                );
                loadFeedbackHighlights();
            } catch (error) {
                setFormStatus(
                    formStatus,
                    "暂时无法连接后端服务，请确认 Java 后端已经启动。",
                    "error"
                );
            } finally {
                if (submitButton) {
                    submitButton.disabled = false;
                    submitButton.removeAttribute("aria-busy");
                }
            }
        });
    }

    function handleViewportChange() {
        if (!isMobileViewport()) {
            closeMenu();
            if (userStoppedCarousel && !document.hidden) {
                startCarousel();
            }
        }

        updateBackToHomeButton();
    }

    highlightCurrentNav();
    if (window.location.hash) {
        window.setTimeout(function () {
            scrollToHashTarget(window.location.hash, false);
        }, 0);
    }
    updateBackToHomeButton();
    loadFeedbackHighlights();

    window.addEventListener("hashchange", function () {
        if (window.location.hash) {
            window.setTimeout(function () {
                scrollToHashTarget(window.location.hash, false);
            }, 0);
        }

        highlightCurrentNav();
        updateBackToHomeButton();
    });

    window.addEventListener("scroll", updateBackToHomeButton, { passive: true });
    window.addEventListener("resize", handleViewportChange);
});
