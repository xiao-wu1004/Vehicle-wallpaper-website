document.addEventListener("DOMContentLoaded", function () {
    const body = document.body;
    const mobileMenu = document.getElementById("mobile-menu");
    const navList = document.getElementById("nav-list");
    const navElement = document.querySelector("nav");
    const navDisclosureButtons = Array.prototype.slice.call(document.querySelectorAll(".nav-disclosure"));
    const header = document.querySelector("header");
    const backToHomeButton = document.getElementById("backToHomeButton");
    const gallerySection = document.getElementById("gallery");
    const brandNavigation = document.getElementById("brands");
    const brandChipList = document.querySelector(".brand-chip-list");
    const brandSubmenu = document.getElementById("brand-submenu");
    const carouselContainer = document.querySelector(".carousel");
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");
    const modal = document.getElementById("myModal");
    const modalPlaceholder = document.getElementById("modalPlaceholder");
    const modalHires = document.getElementById("modalHires");
    const modalImageContainer = document.querySelector(".modal-image-container");
    const downloadBtn = document.getElementById("downloadBtn");
    const modalFavoriteButton = document.getElementById("modalFavoriteButton");
    const closeBtn = document.querySelector(".close");
    const form = document.querySelector("#contact form");
    const feedbackBlockquote = document.querySelector("#feedback blockquote");
    const loginForm = document.getElementById("loginForm");
    const registerForm = document.getElementById("registerForm");
    const loginEmailInput = document.getElementById("loginEmail");
    const loginPasswordInput = document.getElementById("loginPassword");
    const registerDisplayNameInput = document.getElementById("registerDisplayName");
    const registerEmailInput = document.getElementById("registerEmail");
    const registerPasswordInput = document.getElementById("registerPassword");
    const rememberLoginCheckbox = document.getElementById("rememberLoginCheckbox");
    const rememberRegisterCheckbox = document.getElementById("rememberRegisterCheckbox");
    const authTabButtons = Array.prototype.slice.call(document.querySelectorAll("[data-auth-tab]"));
    const authSwitchButtons = Array.prototype.slice.call(document.querySelectorAll("[data-auth-switch]"));
    const passwordToggleButtons = Array.prototype.slice.call(document.querySelectorAll("[data-password-target]"));
    const accountForms = document.getElementById("accountForms");
    const accountStatusCopy = document.getElementById("accountStatusCopy");
    const accountIdentity = document.getElementById("accountIdentity");
    const accountDisplayName = document.getElementById("accountDisplayName");
    const accountEmail = document.getElementById("accountEmail");
    const logoutButton = document.getElementById("logoutButton");
    const accountFavoriteCount = document.getElementById("accountFavoriteCount");
    const accountDownloadCount = document.getElementById("accountDownloadCount");
    const accountFavoritesList = document.getElementById("accountFavoritesList");
    const accountDownloadsList = document.getElementById("accountDownloadsList");
    const defaultApiBase = window.location.protocol === "file:" ? "http://localhost:8080" : "";
    const utf8Decoder = typeof TextDecoder === "function" ? new TextDecoder("utf-8", { fatal: true }) : null;
    const brandNameFallbacks = {
        benz: "奔驰",
        porsche: "保时捷",
        hongqi: "红旗",
        xiaomi: "小米",
        bmw: "宝马",
        audi: "奥迪",
        ferrari: "法拉利",
        lamborghini: "兰博基尼",
        astonmartin: "阿斯顿马丁",
        maserati: "玛莎拉蒂",
        bugatti: "布加迪",
        ford: "福特"
    };
    const contactHashes = new Set(["#contact", "#feedback"]);
    const scrollLocks = new Set();
    const preloadCache = Object.create(null);
    const savedVisitorKeyStorageName = "vehicleWallpaperVisitorKey";
    const savedUserTokenStorageName = "vehicleWallpaperUserToken";
    const authState = {
        visitorKey: "",
        accessToken: "",
        currentUser: null,
        profile: null
    };

    let dynamicBrandHashes = new Set(["#brands"]);
    let modalScale = 1;
    let carouselIndex = 0;
    let carouselTimer = null;
    let userStoppedCarousel = false;
    let catalogStatusElement = null;
    let currentModalWallpaper = null;
    let currentAuthMode = "login";
    const initialGalleryBrandSectionMarkup = gallerySection
        ? Array.prototype.slice.call(gallerySection.querySelectorAll(".brand-gallery")).map(function (section) {
            return section.outerHTML;
        })
        : [];
    const initialGalleryBrandCount = initialGalleryBrandSectionMarkup.length;
    let lastCatalogRenderMode = initialGalleryBrandCount ? "static" : "";

    function normalizeValue(value) {
        return value == null ? "" : String(value).trim();
    }

    function looksLikeMojibake(value) {
        return /[\u0080-\u009fÃâ�]/.test(value);
    }

    function repairPotentialMojibake(value) {
        const input = normalizeValue(value);
        if (!input || !utf8Decoder || !looksLikeMojibake(input)) {
            return input;
        }

        try {
            const bytes = Uint8Array.from(Array.from(input), function (character) {
                return character.charCodeAt(0) & 255;
            });
            const decoded = utf8Decoder.decode(bytes);
            if (decoded && decoded !== input && (!looksLikeMojibake(decoded) || /[\u4e00-\u9fff]/.test(decoded))) {
                return decoded;
            }
        } catch (error) {
            return input;
        }

        return input;
    }

    function displayText(value, fallback) {
        const repaired = repairPotentialMojibake(value);
        return repaired || fallback || "";
    }

    function basename(fileName) {
        const normalized = normalizeValue(fileName);
        if (!normalized) {
            return "";
        }
        return normalized.replace(/\.[^.]+$/, "");
    }

    function resolveBrandName(brand) {
        const slug = normalizeValue(brand && brand.slug).toLowerCase();
        return displayText(brand && (brand.name || brand.displayName), brandNameFallbacks[slug] || slug || "未命名品牌");
    }

    function resolveWallpaperTitle(brandName, wallpaper) {
        const title = displayText(wallpaper && wallpaper.title);
        if (title) {
            return title;
        }

        const fileNameFallback = displayText(basename(wallpaper && wallpaper.fileName), wallpaper && (wallpaper.id || "壁纸"));
        return fileNameFallback || (brandName + "壁纸");
    }

    function getApiBase() {
        const configuredApiBase = body.getAttribute("data-api-base")
            || (window.VEHICLE_WALLPAPER_CONFIG && window.VEHICLE_WALLPAPER_CONFIG.apiBase)
            || "";

        if (configuredApiBase) {
            return String(configuredApiBase).trim().replace(/\/$/, "");
        }

        return defaultApiBase;
    }

    function buildApiUrl(path) {
        return getApiBase() + path;
    }

    function generateRandomHex(byteLength) {
        if (window.crypto && typeof window.crypto.getRandomValues === "function") {
            const bytes = new Uint8Array(byteLength);
            window.crypto.getRandomValues(bytes);
            return Array.prototype.map.call(bytes, function (byteValue) {
                return ("0" + byteValue.toString(16)).slice(-2);
            }).join("");
        }

        let fallback = "";
        while (fallback.length < byteLength * 2) {
            fallback += Math.random().toString(16).slice(2);
        }
        return fallback.slice(0, byteLength * 2);
    }

    function ensureVisitorKey() {
        const savedKey = normalizeValue(localStorage.getItem(savedVisitorKeyStorageName));
        if (savedKey && /^[A-Za-z0-9_-]{16,96}$/.test(savedKey)) {
            authState.visitorKey = savedKey;
            return savedKey;
        }

        const generatedKey = "visitor_" + generateRandomHex(16);
        localStorage.setItem(savedVisitorKeyStorageName, generatedKey);
        authState.visitorKey = generatedKey;
        return generatedKey;
    }

    function syncRememberCheckboxes(shouldPersist) {
        const normalizedPreference = shouldPersist !== false;
        if (rememberLoginCheckbox) {
            rememberLoginCheckbox.checked = normalizedPreference;
        }
        if (rememberRegisterCheckbox) {
            rememberRegisterCheckbox.checked = normalizedPreference;
        }
    }

    function restoreAccessToken() {
        const localToken = normalizeValue(localStorage.getItem(savedUserTokenStorageName));
        const sessionToken = normalizeValue(sessionStorage.getItem(savedUserTokenStorageName));
        authState.accessToken = localToken || sessionToken;

        if (localToken) {
            syncRememberCheckboxes(true);
        } else if (sessionToken) {
            syncRememberCheckboxes(false);
        }

        return authState.accessToken;
    }

    function rememberAccessToken(token, shouldPersist) {
        authState.accessToken = normalizeValue(token);
        localStorage.removeItem(savedUserTokenStorageName);
        sessionStorage.removeItem(savedUserTokenStorageName);

        if (!authState.accessToken) {
            return;
        }

        const targetStorage = shouldPersist === false ? sessionStorage : localStorage;
        targetStorage.setItem(savedUserTokenStorageName, authState.accessToken);
        syncRememberCheckboxes(targetStorage === localStorage);
    }

    function clearAccessToken() {
        authState.currentUser = null;
        rememberAccessToken("");
    }

    function buildApiHeaders(extraHeaders) {
        const headers = Object.assign({
            Accept: "application/json"
        }, extraHeaders || {});

        if (authState.visitorKey) {
            headers["X-Visitor-Key"] = authState.visitorKey;
        }

        if (authState.accessToken) {
            headers.Authorization = "Bearer " + authState.accessToken;
        }

        return headers;
    }

    async function requestJson(path, options) {
        const requestOptions = Object.assign({}, options || {});
        requestOptions.headers = buildApiHeaders(requestOptions.headers);

        const response = await fetch(buildApiUrl(path), requestOptions);
        const payload = await response.json().catch(function () {
            return {};
        });

        if (!response.ok) {
            const error = new Error(payload.message || "Request failed.");
            error.status = response.status;
            error.fieldErrors = payload.fieldErrors || {};
            throw error;
        }

        return payload;
    }

    function resolveAssetUrl(path) {
        const normalizedPath = normalizeValue(path);
        if (!normalizedPath) {
            return "";
        }
        if (/^https?:\/\//i.test(normalizedPath)) {
            return normalizedPath;
        }
        if (normalizedPath.charAt(0) === "/") {
            return buildApiUrl(normalizedPath);
        }
        return buildApiUrl("/" + normalizedPath);
    }

    function buildWallpaperDownloadUrl(path) {
        const normalizedPath = normalizeValue(path);
        if (!normalizedPath) {
            return "";
        }

        if (/^https?:\/\//i.test(normalizedPath)) {
            try {
                const parsedUrl = new URL(normalizedPath);
                if (parsedUrl.pathname.indexOf("/download/") === 0) {
                    return parsedUrl.toString();
                }
                if (parsedUrl.pathname.indexOf("/cars/") === 0) {
                    parsedUrl.pathname = "/download" + parsedUrl.pathname.substring(5);
                }
                return parsedUrl.toString();
            } catch (error) {
                return normalizedPath;
            }
        }

        if (normalizedPath.indexOf("/download/") === 0) {
            return buildApiUrl(normalizedPath);
        }
        if (normalizedPath.indexOf("/cars/") === 0) {
            return buildApiUrl("/download" + normalizedPath.substring(5));
        }
        if (normalizedPath.indexOf("cars/") === 0) {
            return buildApiUrl("/download/" + normalizedPath.substring(5));
        }

        return buildApiUrl(normalizedPath.charAt(0) === "/" ? normalizedPath : "/" + normalizedPath);
    }

    function isMobileViewport() {
        return window.matchMedia("(max-width: 768px)").matches;
    }

    function getNavLinks() {
        return Array.prototype.slice.call(document.querySelectorAll("nav a"));
    }

    function getCarouselImages() {
        return carouselContainer
            ? Array.prototype.slice.call(carouselContainer.querySelectorAll(".carousel-image"))
            : [];
    }

    function ensureCatalogStatusElement() {
        if (!gallerySection) {
            return null;
        }

        if (!catalogStatusElement) {
            catalogStatusElement = document.getElementById("catalogStatus");
        }

        if (catalogStatusElement) {
            return catalogStatusElement;
        }

        catalogStatusElement = document.createElement("p");
        catalogStatusElement.id = "catalogStatus";
        catalogStatusElement.className = "catalog-status";
        catalogStatusElement.hidden = true;

        if (brandNavigation && brandNavigation.parentNode === gallerySection) {
            gallerySection.insertBefore(catalogStatusElement, brandNavigation.nextSibling);
        } else if (gallerySection.firstChild) {
            gallerySection.appendChild(catalogStatusElement);
        }

        return catalogStatusElement;
    }

    function setCatalogStatus(message, tone) {
        const element = ensureCatalogStatusElement();
        if (!element) {
            return;
        }

        const normalizedMessage = normalizeValue(message);
        element.className = "catalog-status";

        if (!normalizedMessage) {
            element.hidden = true;
            element.textContent = "";
            return;
        }

        element.hidden = false;
        element.textContent = normalizedMessage;
        if (tone === "error") {
            element.classList.add("is-error");
        } else if (tone === "loading") {
            element.classList.add("is-loading");
        }
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
        if (!parent) {
            return;
        }

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
        if (!mobileMenu || !navList) {
            return;
        }

        navList.classList.remove("active");
        mobileMenu.setAttribute("aria-expanded", "false");
        collapseAllDisclosures();
        removeScrollLock("menu");
    }

    function openMenu() {
        if (!mobileMenu || !navList) {
            return;
        }

        navList.classList.add("active");
        mobileMenu.setAttribute("aria-expanded", "true");
        if (isMobileViewport()) {
            addScrollLock("menu");
        }
    }

    function toggleMenu() {
        if (!mobileMenu || !navList) {
            return;
        }

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

        getNavLinks().forEach(function (link) {
            const href = link.getAttribute("href") || "";
            let isActive = false;

            if (href.charAt(0) === "#") {
                isActive = href === activeHash;

                if (!isActive && href === "#contact" && contactHashes.has(activeHash)) {
                    isActive = true;
                }

                if (!isActive && href === "#brands" && dynamicBrandHashes.has(activeHash)) {
                    isActive = true;
                }
            } else if (currentFile) {
                isActive = currentFile === href.toLowerCase();
            }

            link.classList.toggle("active", isActive);
        });
    }

    function updateBackToHomeButton() {
        if (!backToHomeButton) {
            return;
        }

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
        if (!hash || hash === "#") {
            return false;
        }

        const target = document.querySelector(hash);
        if (!target) {
            return false;
        }

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
        const previewSrc = normalizeValue(card && card.dataset && card.dataset.preview);
        if (previewSrc) {
            return previewSrc;
        }

        const image = card.querySelector("img");
        if (!image) {
            return "";
        }
        return image.currentSrc || image.getAttribute("src") || "";
    }

    function getWallpaperDataFromElement(element) {
        if (!element) {
            return null;
        }

        return {
            wallpaperId: normalizeValue(element.dataset.wallpaperId),
            title: normalizeValue(element.dataset.title),
            brandName: normalizeValue(element.dataset.brand),
            previewSrc: normalizeValue(element.dataset.preview) || getPreviewSrc(element),
            fullSrc: normalizeValue(element.dataset.full),
            favorited: element.dataset.favorited === "true"
        };
    }

    function setFavoriteButtonState(button, favorited) {
        if (!button) {
            return;
        }

        const isFavorited = Boolean(favorited);
        button.dataset.favorited = isFavorited ? "true" : "false";
        button.classList.toggle("is-active", isFavorited);
        button.textContent = isFavorited ? "已收藏" : "收藏";
    }

    function setButtonBusy(button, isBusy) {
        if (!button) {
            return;
        }

        button.disabled = Boolean(isBusy);
        if (isBusy) {
            button.setAttribute("aria-busy", "true");
        } else {
            button.removeAttribute("aria-busy");
        }
    }

    function getHiResPreviewSrc(fullSrc) {
        if (!fullSrc) {
            return "";
        }
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
        if (!modal || !modalPlaceholder || !modalHires || !downloadBtn) {
            return;
        }

        modal.classList.remove("show");
        modal.setAttribute("aria-hidden", "true");
        modalHires.classList.remove("loaded");
        modalHires.src = "";
        modalPlaceholder.src = "";
        modalScale = 1;
        modalHires.style.transform = "scale(1)";
        downloadBtn.style.display = "none";
        downloadBtn.href = "#";
        downloadBtn.dataset.wallpaperId = "";
        currentModalWallpaper = null;
        if (modalFavoriteButton) {
            modalFavoriteButton.hidden = true;
            modalFavoriteButton.dataset.wallpaperId = "";
            setFavoriteButtonState(modalFavoriteButton, false);
        }
        removeScrollLock("modal");
    }

    function openModal(card) {
        if (!modal || !modalPlaceholder || !modalHires || !downloadBtn) {
            return;
        }

        const wallpaperData = getWallpaperDataFromElement(card);
        if (!wallpaperData) {
            return;
        }

        const fullSrc = wallpaperData.fullSrc || "";
        const hiResSrc = getHiResPreviewSrc(fullSrc) || fullSrc;
        const previewSrc = wallpaperData.previewSrc || getPreviewSrc(card);
        currentModalWallpaper = wallpaperData;

        modal.classList.add("show");
        modal.setAttribute("aria-hidden", "false");
        addScrollLock("modal");

        modalScale = 1;
        modalHires.style.transform = "scale(1)";
        modalHires.classList.remove("loaded");
        modalHires.src = "";

        modalPlaceholder.src = previewSrc;
        // 下载链接统一走 /download 端点，避免完整图片地址在跨域场景下变成直接预览
        downloadBtn.href = buildWallpaperDownloadUrl(fullSrc);
        const downloadFileName = decodeURIComponent((fullSrc.split("/").pop() || "wallpaper"));
        downloadBtn.setAttribute("download", downloadFileName);
        downloadBtn.dataset.wallpaperId = wallpaperData.wallpaperId || "";
        downloadBtn.style.display = "inline-flex";

        if (modalFavoriteButton && wallpaperData.wallpaperId) {
            modalFavoriteButton.hidden = false;
            modalFavoriteButton.dataset.wallpaperId = wallpaperData.wallpaperId;
            setFavoriteButtonState(modalFavoriteButton, wallpaperData.favorited);
        } else if (modalFavoriteButton) {
            modalFavoriteButton.hidden = true;
            modalFavoriteButton.dataset.wallpaperId = "";
        }

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
        const carouselImages = getCarouselImages();
        carouselImages.forEach(function (image, imageIndex) {
            const isActive = imageIndex === index;
            image.style.opacity = isActive ? "1" : "0";
            image.classList.toggle("active", isActive);
            image.setAttribute("aria-hidden", isActive ? "false" : "true");
        });
    }

    function showNextCarouselImage() {
        const carouselImages = getCarouselImages();
        if (!carouselImages.length) {
            return;
        }
        carouselIndex = (carouselIndex + 1) % carouselImages.length;
        showCarouselImage(carouselIndex);
    }

    function showPreviousCarouselImage() {
        const carouselImages = getCarouselImages();
        if (!carouselImages.length) {
            return;
        }
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
        const carouselImages = getCarouselImages();
        if (!carouselImages.length) {
            return;
        }
        if (userStoppedCarousel && isMobileViewport()) {
            return;
        }

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
        if (!field) {
            return;
        }

        const hasError = Boolean(message);
        field.element.setAttribute("aria-invalid", hasError ? "true" : "false");
        field.error.textContent = message || "";
    }

    function validateField(field) {
        const value = field.element.value.trim();

        if (field.element.id === "name" && !value) {
            setFieldState(field, "请输入姓名。");
            return false;
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

        if (field.element.id === "message" && !value) {
            setFieldState(field, "请输入留言内容。");
            return false;
        }

        setFieldState(field, "");
        return true;
    }

    function ensureFormStatusElement(formElement) {
        if (!formElement) {
            return null;
        }

        let statusElement = formElement.querySelector(".form-status");
        if (statusElement) {
            return statusElement;
        }

        statusElement = document.createElement("p");
        statusElement.className = "form-status";
        statusElement.setAttribute("role", "status");
        statusElement.setAttribute("aria-live", "polite");
        // 鎻掑叆鍒版彁浜ゆ寜閽箣鍓嶏紝纭繚鍙
        const submitButton = formElement.querySelector(".form-submit");
        if (submitButton) {
            formElement.insertBefore(statusElement, submitButton);
        } else {
            formElement.appendChild(statusElement);
        }
        return statusElement;
    }

    function legacyFriendlyAuthError(message) {
        return friendlyAuthError(message, null);
    }

    function firstFieldError(fieldErrors) {
        if (!fieldErrors || typeof fieldErrors !== "object") {
            return "";
        }

        const keys = Object.keys(fieldErrors);
        if (!keys.length) {
            return "";
        }

        return normalizeValue(fieldErrors[keys[0]]);
    }

    function friendlyAuthError(message, fieldErrors) {
        const sourceMessage = firstFieldError(fieldErrors) || normalizeValue(message);

        if (!sourceMessage) {
            return "操作失败，请稍后重试。";
        }

        const lower = sourceMessage.toLowerCase();
        if (lower.includes("too many") && lower.includes("registration")) {
            return "注册请求过于频繁，请 15 分钟后再试。";
        }
        if (lower.includes("too many") && lower.includes("login")) {
            return "登录尝试次数过多，请 15 分钟后再试。";
        }
        if (lower.includes("temporarily locked")) {
            return "该账号因多次登录失败已被临时锁定，请 30 分钟后再试。";
        }
        if (lower.includes("incorrect email or password") || lower.includes("email or password")) {
            return "邮箱或密码不正确，请检查后重试。";
        }
        if (lower.includes("already registered") || lower.includes("already exists")) {
            return "该邮箱已注册，请直接登录或更换其他邮箱。";
        }
        if (lower.includes("valid email address")) {
            return "请输入有效的邮箱地址。";
        }
        if (lower.includes("display name is required")) {
            return "请输入昵称。";
        }
        if (lower.includes("display name") && lower.includes("120")) {
            return "昵称最多 120 个字符。";
        }
        if (lower.includes("email is required")) {
            return "请输入邮箱地址。";
        }
        if (lower.includes("email must be 160")) {
            return "邮箱长度不能超过 160 个字符。";
        }
        if (lower.includes("password is required")) {
            return "请输入密码。";
        }
        if (lower.includes("password") && (lower.includes("8") || lower.includes("letter") || lower.includes("number"))) {
            return "密码需要至少 8 位，并同时包含字母和数字。";
        }
        if (lower.includes("visitor key is required")) {
            return "请先登录后再操作。";
        }

        return sourceMessage;
    }

    function isValidEmailAddress(value) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizeValue(value));
    }

    function getFriendlyAuthError(message, fieldErrors) {
        return friendlyAuthError(message, fieldErrors);
    }

    function refreshPasswordToggleButton(button) {
        if (!button) {
            return;
        }

        const targetInput = document.getElementById(button.dataset.passwordTarget || "");
        const isRevealed = Boolean(targetInput && targetInput.type === "text");
        button.textContent = isRevealed ? "隐藏" : "显示";
        button.setAttribute("aria-label", isRevealed ? "隐藏密码" : "显示密码");
        button.setAttribute("aria-pressed", isRevealed ? "true" : "false");
    }

    function syncPasswordHintState(value) {
        const normalizedValue = normalizeValue(value);
        const hintLength = document.getElementById("hintLength");
        const hintLetter = document.getElementById("hintLetter");
        const hintNumber = document.getElementById("hintNumber");

        toggleHint(hintLength, normalizedValue.length >= 8);
        toggleHint(hintLetter, /[A-Za-z]/.test(normalizedValue));
        toggleHint(hintNumber, /[0-9]/.test(normalizedValue));
    }

    function moveAuthTabFocus(direction) {
        if (authTabButtons.length < 2) {
            return;
        }

        const activeIndex = authTabButtons.findIndex(function (button) {
            return button.dataset.authTab === currentAuthMode;
        });
        const fallbackIndex = activeIndex >= 0 ? activeIndex : 0;
        const nextIndex = (fallbackIndex + direction + authTabButtons.length) % authTabButtons.length;
        const nextButton = authTabButtons[nextIndex];
        if (!nextButton) {
            return;
        }

        switchAuthMode(nextButton.dataset.authTab, { focus: false });
        nextButton.focus();
    }

    function setInputInvalid(input, shouldMarkInvalid) {
        if (!input) {
            return;
        }

        if (shouldMarkInvalid) {
            input.setAttribute("aria-invalid", "true");
            return;
        }

        input.removeAttribute("aria-invalid");
    }

    function applyAuthFieldErrors(fieldErrors, fieldMap) {
        Object.keys(fieldMap).forEach(function (key) {
            setInputInvalid(fieldMap[key], Boolean(fieldErrors && fieldErrors[key]));
        });
    }

    function updatePasswordToggleButton(button) {
        refreshPasswordToggleButton(button);
    }

    function resetPasswordVisibility() {
        passwordToggleButtons.forEach(function (button) {
            const targetInput = document.getElementById(button.dataset.passwordTarget || "");
            if (!targetInput) {
                return;
            }

            targetInput.type = "password";
            refreshPasswordToggleButton(button);
        });
    }

    function syncAuthEmails(sourceInput) {
        const normalizedEmail = normalizeValue(sourceInput && sourceInput.value);
        if (!normalizedEmail) {
            return;
        }

        if (sourceInput === loginEmailInput && registerEmailInput && !normalizeValue(registerEmailInput.value)) {
            registerEmailInput.value = normalizedEmail;
        }

        if (sourceInput === registerEmailInput && loginEmailInput && !normalizeValue(loginEmailInput.value)) {
            loginEmailInput.value = normalizedEmail;
        }
    }

    function switchAuthMode(mode, options) {
        const normalizedMode = mode === "register" ? "register" : "login";
        const config = options || {};
        currentAuthMode = normalizedMode;

        if (normalizedMode === "register") {
            syncAuthEmails(loginEmailInput);
        } else {
            syncAuthEmails(registerEmailInput);
        }

        authTabButtons.forEach(function (button) {
            const isActive = button.dataset.authTab === normalizedMode;
            button.classList.toggle("is-active", isActive);
            button.setAttribute("aria-selected", isActive ? "true" : "false");
            button.tabIndex = isActive ? 0 : -1;
        });

        if (loginForm) {
            loginForm.hidden = normalizedMode !== "login";
        }
        if (registerForm) {
            registerForm.hidden = normalizedMode !== "register";
        }

        if (config.clearStatus !== false) {
            setFormStatus(loginForm && loginForm.querySelector(".form-status"), "", "");
            setFormStatus(registerForm && registerForm.querySelector(".form-status"), "", "");
        }

        if (!config.focus) {
            return;
        }

        const firstField = normalizedMode === "register" ? registerDisplayNameInput : loginEmailInput;
        if (firstField) {
            firstField.focus();
        }
    }

    function triggerFileDownload(url, fileName) {
        // /download 端点已带 Content-Disposition: attachment，浏览器直接下载
        var anchor = document.createElement("a");
        anchor.href = url;
        anchor.download = fileName;
        anchor.style.display = "none";
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
    }

    function toggleHint(element, isMet) {
        if (!element) return;
        element.classList.toggle("met", isMet);
    }

    function setFormStatus(statusElement, message, type) {
        if (!statusElement) {
            if (message) {
                window.alert(message);
            }
            return;
        }

        statusElement.textContent = message || "";
        statusElement.classList.remove("is-success", "is-error");
        statusElement.classList.remove("is-success", "is-error");

        if (type === "error") {
            statusElement.classList.add("is-error");
            // 濡傛灉閿欒娑堟伅鍦ㄨ鍙ｅ锛屾粴鍔ㄥ埌鍙鍖哄煙
            setTimeout(function () {
                if (statusElement.getBoundingClientRect().top > window.innerHeight
                    || statusElement.getBoundingClientRect().bottom < 0) {
                    statusElement.scrollIntoView({ behavior: "smooth", block: "center" });
                }
            }, 100);
        } else if (type === "success") {
            statusElement.classList.add("is-success");
        }

        if (type === "success") {
            statusElement.classList.add("is-success");
        }

        if (type === "error") {
            statusElement.classList.add("is-error");
        }
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
            paragraph.textContent = "\"" + displayText(item.message, "这条精选反馈还没有正文。") + "\" - " + displayText(item.name, "匿名用户");
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
                    throw new Error("Failed to load feedback highlights.");
                }
                return response.json();
            })
            .then(renderFeedbackHighlights)
            .catch(function () {
                // Keep the existing fallback quotes when the backend is unavailable.
            });
    }

    function createBrandMenuLink(hash, label) {
        const link = document.createElement("a");
        link.href = hash;
        link.textContent = label;
        return link;
    }

    function createWallpaperCard(brandName, wallpaper, index) {
        const title = resolveWallpaperTitle(brandName, wallpaper);
        const article = document.createElement("article");
        article.className = "wallpaper-card";
        const button = document.createElement("button");
        button.className = "image-card";
        button.type = "button";
        button.setAttribute("aria-label", "预览" + brandName + "壁纸" + (index + 1));
        button.dataset.full = resolveAssetUrl(wallpaper.fullUrl || wallpaper.downloadUrl || wallpaper.previewUrl);
        button.dataset.preview = resolveAssetUrl(wallpaper.previewUrl || wallpaper.fullUrl);
        button.dataset.wallpaperId = normalizeValue(wallpaper.id);
        button.dataset.title = title;
        button.dataset.brand = brandName;
        button.dataset.favorited = wallpaper.favorited ? "true" : "false";

        const image = document.createElement("img");
        image.src = resolveAssetUrl(wallpaper.previewUrl || wallpaper.fullUrl);
        image.alt = title;
        image.loading = "lazy";
        image.decoding = "async";

        button.appendChild(image);

        const body = document.createElement("div");
        body.className = "wallpaper-card-body";

        const copy = document.createElement("div");
        copy.className = "wallpaper-card-copy";

        const titleElement = document.createElement("h5");
        titleElement.className = "wallpaper-card-title";
        titleElement.textContent = title;

        const meta = document.createElement("p");
        meta.className = "wallpaper-card-meta";
        meta.textContent = (wallpaper.favoriteCount || 0) + " 收藏 / " + (wallpaper.downloadCount || 0) + " 下载";

        copy.appendChild(titleElement);
        copy.appendChild(meta);

        const favoriteButton = document.createElement("button");
        favoriteButton.className = "favorite-toggle";
        favoriteButton.type = "button";
        favoriteButton.dataset.wallpaperId = normalizeValue(wallpaper.id);
        setFavoriteButtonState(favoriteButton, Boolean(wallpaper.favorited));

        body.appendChild(copy);
        body.appendChild(favoriteButton);

        article.appendChild(button);
        article.appendChild(body);
        return article;
    }

    function clearDynamicCatalogSections() {
        if (!gallerySection) {
            return;
        }

        Array.prototype.slice.call(gallerySection.querySelectorAll(".brand-gallery")).forEach(function (section) {
            section.parentNode.removeChild(section);
        });
    }

    function removeGalleryLoginGate() {
        if (!gallerySection) {
            return;
        }

        const existingGate = gallerySection.querySelector(".gallery-login-gate");
        if (existingGate) {
            existingGate.parentNode.removeChild(existingGate);
        }
    }

    function restoreStaticCatalogSections() {
        if (!gallerySection || !initialGalleryBrandSectionMarkup.length) {
            return;
        }

        clearDynamicCatalogSections();
        removeGalleryLoginGate();

        initialGalleryBrandSectionMarkup.forEach(function (markup) {
            const template = document.createElement("template");
            template.innerHTML = markup;
            if (template.content.firstElementChild) {
                gallerySection.appendChild(template.content.firstElementChild);
            }
        });

        lastCatalogRenderMode = "static";
    }

    function renderGalleryLoginGate(brandCount) {
        if (!gallerySection) {
            return;
        }

        clearDynamicCatalogSections();
        removeGalleryLoginGate();

        // 绉婚櫎宸插瓨鍦ㄧ殑鐧诲綍闂ㄧ鍗＄墖锛堥伩鍏嶉噸澶嶏級
        const existingGate = gallerySection.querySelector(".gallery-login-gate");
        if (existingGate) {
            existingGate.parentNode.removeChild(existingGate);
        }

        const gate = document.createElement("div");
        gate.className = "gallery-login-gate";
        gate.innerHTML =
            '<div class="gallery-login-gate-card">'
            + '<p class="gallery-login-gate-icon">🔐</p>'
            + '<h3>登录后查看全部壁纸</h3>'
            + '<p>我们收录了 <strong>12</strong> 个品牌的精选壁纸，注册即可浏览和下载。</p>'
            + '<button type="button" class="form-submit gallery-login-gate-button">去登录 / 注册</button>'
            + '</div>';

        gate.querySelector(".gallery-login-gate-button").addEventListener("click", function () {
            const accountSection = document.getElementById("account");
            if (accountSection) {
                accountSection.scrollIntoView({ behavior: "smooth" });
            }
            switchAuthMode("login", { focus: false });
            const loginInput = document.getElementById("loginEmail");
            if (loginInput) {
                setTimeout(function () { loginInput.focus(); }, 500);
            }
        });

        gallerySection.appendChild(gate);
        lastCatalogRenderMode = "gate";
    }

    function renderBrandNavigation(brands) {
        if (brandSubmenu) {
            brandSubmenu.innerHTML = "";
            brands.forEach(function (brand) {
                const hash = "#" + brand.slug;
                brandSubmenu.appendChild(createBrandMenuLink(hash, resolveBrandName(brand)));
            });
        }

        if (brandChipList) {
            brandChipList.innerHTML = "";
            brands.forEach(function (brand) {
                const listItem = document.createElement("li");
                listItem.appendChild(createBrandMenuLink("#" + brand.slug, resolveBrandName(brand)));
                brandChipList.appendChild(listItem);
            });
        }
    }

    function renderBrandSections(brands) {
        if (!gallerySection) {
            return;
        }

        clearDynamicCatalogSections();
        removeGalleryLoginGate();

        brands.forEach(function (brand) {
            const brandName = resolveBrandName(brand);
            const section = document.createElement("div");
            section.id = brand.slug;
            section.className = "brand-gallery";

            const heading = document.createElement("h4");
            heading.textContent = brandName;
            section.appendChild(heading);

            const grid = document.createElement("div");
            grid.className = "image-grid";

            if (!Array.isArray(brand.wallpapers) || brand.wallpapers.length === 0) {
                const emptyState = document.createElement("p");
                emptyState.className = "brand-gallery-empty";
                emptyState.textContent = "这个品牌的公开壁纸还没有上线。";
                section.appendChild(emptyState);
            } else {
                brand.wallpapers.forEach(function (wallpaper, index) {
                    grid.appendChild(createWallpaperCard(brandName, wallpaper, index));
                });
                section.appendChild(grid);
            }

            gallerySection.appendChild(section);
        });

        lastCatalogRenderMode = "catalog";
    }

    function buildCarouselCandidates(brands) {
        const highlights = [];
        const seenIds = Object.create(null);

        brands.forEach(function (brand) {
            if (Array.isArray(brand.wallpapers) && brand.wallpapers.length > 0) {
                const wallpaper = brand.wallpapers[0];
                const key = normalizeValue(wallpaper.id || wallpaper.fullUrl || wallpaper.previewUrl);
                if (!seenIds[key]) {
                    seenIds[key] = true;
                    highlights.push({
                        brand: brand,
                        wallpaper: wallpaper
                    });
                }
            }
        });

        if (highlights.length >= 8) {
            return highlights.slice(0, 8);
        }

        brands.forEach(function (brand) {
            (brand.wallpapers || []).forEach(function (wallpaper) {
                if (highlights.length >= 10) {
                    return;
                }
                const key = normalizeValue(wallpaper.id || wallpaper.fullUrl || wallpaper.previewUrl);
                if (!seenIds[key]) {
                    seenIds[key] = true;
                    highlights.push({
                        brand: brand,
                        wallpaper: wallpaper
                    });
                }
            });
        });

        return highlights.slice(0, 10);
    }

    function renderCarousel(brands) {
        if (!carouselContainer) {
            return;
        }

        stopCarousel();
        Array.prototype.slice.call(carouselContainer.querySelectorAll(".carousel-image")).forEach(function (image) {
            image.parentNode.removeChild(image);
        });

        const highlights = buildCarouselCandidates(brands);
        if (!highlights.length) {
            return;
        }

        highlights.forEach(function (entry, index) {
            const brandName = resolveBrandName(entry.brand);
            const wallpaper = entry.wallpaper;
            const image = document.createElement("img");
            image.className = "carousel-image";
            image.src = resolveAssetUrl(wallpaper.previewUrl || wallpaper.fullUrl);
            image.alt = resolveWallpaperTitle(brandName, wallpaper);
            image.loading = index < 2 ? "eager" : "lazy";
            image.decoding = "async";
            if (index === 0) {
                image.setAttribute("fetchpriority", "high");
            }

            carouselContainer.insertBefore(image, nextBtn || null);
        });

        carouselIndex = 0;
        userStoppedCarousel = false;
        showCarouselImage(carouselIndex);
        startCarousel();
    }

    function createProfileItem(wallpaper) {
        const button = document.createElement("button");
        button.className = "profile-item";
        button.type = "button";
        button.dataset.full = resolveAssetUrl(wallpaper.fullUrl || wallpaper.downloadUrl || wallpaper.previewUrl);
        button.dataset.preview = resolveAssetUrl(wallpaper.previewUrl || wallpaper.fullUrl);
        button.dataset.wallpaperId = normalizeValue(wallpaper.id);
        button.dataset.title = resolveWallpaperTitle("", wallpaper);
        button.dataset.brand = normalizeValue(wallpaper.brandSlug);
        button.dataset.favorited = wallpaper.favorited ? "true" : "false";

        const image = document.createElement("img");
        image.src = resolveAssetUrl(wallpaper.previewUrl || wallpaper.fullUrl);
        image.alt = resolveWallpaperTitle("", wallpaper);
        image.loading = "lazy";
        image.decoding = "async";

        const copy = document.createElement("div");
        const title = document.createElement("strong");
        title.textContent = resolveWallpaperTitle("", wallpaper);
        const meta = document.createElement("span");
        meta.textContent = (wallpaper.favoriteCount || 0) + " 收藏 / " + (wallpaper.downloadCount || 0) + " 下载";

        copy.appendChild(title);
        copy.appendChild(meta);
        button.appendChild(image);
        button.appendChild(copy);
        return button;
    }

    function renderProfileList(container, items, emptyMessage) {
        if (!container) {
            return;
        }

        container.innerHTML = "";
        if (!Array.isArray(items) || items.length === 0) {
            const emptyState = document.createElement("p");
            emptyState.className = "empty-state";
            emptyState.textContent = emptyMessage;
            container.appendChild(emptyState);
            return;
        }

        items.forEach(function (wallpaper) {
            container.appendChild(createProfileItem(wallpaper));
        });
    }

    function syncAccountUi(profile) {
        const activeProfile = profile || {
            authenticated: false,
            displayName: "",
            email: "",
            favoriteCount: 0,
            downloadCount: 0,
            favorites: [],
            recentDownloads: []
        };
        const authenticated = Boolean(activeProfile.authenticated);

        authState.profile = activeProfile;
        authState.currentUser = authenticated
            ? {
                displayName: displayText(activeProfile.displayName, ""),
                email: normalizeValue(activeProfile.email)
            }
            : null;

        if (accountStatusCopy) {
            accountStatusCopy.textContent = authenticated
                ? "已登录，收藏和下载记录会随账号同步。"
                : "当前以游客身份浏览，收藏和下载记录仅保存在本机浏览器。";
        }

        if (accountForms) {
            accountForms.hidden = authenticated;
        }

        if (accountIdentity) {
            accountIdentity.hidden = !authenticated;
        }

        if (accountDisplayName) {
            accountDisplayName.textContent = displayText(activeProfile.displayName, "已登录用户");
        }

        if (accountEmail) {
            accountEmail.textContent = normalizeValue(activeProfile.email);
        }

        if (logoutButton) {
            logoutButton.hidden = !authenticated;
        }

        if (accountFavoriteCount) {
            accountFavoriteCount.textContent = String(activeProfile.favoriteCount || 0);
        }

        if (accountDownloadCount) {
            accountDownloadCount.textContent = String(activeProfile.downloadCount || 0);
        }

        renderProfileList(
            accountFavoritesList,
            activeProfile.favorites,
            authenticated ? "还没有收藏任何壁纸，去壁纸库挑一张吧。" : "游客收藏会显示在这里。"
        );
        renderProfileList(
            accountDownloadsList,
            activeProfile.recentDownloads,
            authenticated ? "还没有下载记录，打开任意壁纸即可开始积累。" : "最近下载会显示在这里。"
        );
    }

    function buildAuthenticatedProfileFallback() {
        const previousProfile = authState.profile && authState.profile.authenticated ? authState.profile : null;
        return {
            authenticated: true,
            displayName: displayText(authState.currentUser && authState.currentUser.displayName, "已登录用户"),
            email: normalizeValue(authState.currentUser && authState.currentUser.email),
            favoriteCount: previousProfile ? previousProfile.favoriteCount || 0 : 0,
            downloadCount: previousProfile ? previousProfile.downloadCount || 0 : 0,
            favorites: previousProfile && Array.isArray(previousProfile.favorites) ? previousProfile.favorites : [],
            recentDownloads: previousProfile && Array.isArray(previousProfile.recentDownloads) ? previousProfile.recentDownloads : []
        };
    }

    async function syncAuthStatus() {
        restoreAccessToken();

        if (!authState.accessToken) {
            authState.currentUser = null;
            return;
        }

        try {
            const payload = await requestJson("/api/auth/me");
            if (!payload.authenticated) {
                clearAccessToken();
                return;
            }

            authState.currentUser = {
                displayName: displayText(payload.displayName, ""),
                email: normalizeValue(payload.email)
            };
        } catch (error) {
            // Keep the last known token in storage when the backend is temporarily unavailable.
        }
    }

    async function loadProfile() {
        if (!accountFavoriteCount || !accountDownloadCount) {
            return;
        }

        try {
            const profile = await requestJson("/api/catalog/me");
            syncAccountUi(profile);
        } catch (error) {
            if (authState.accessToken && authState.currentUser) {
                syncAccountUi(buildAuthenticatedProfileFallback());
                if (accountStatusCopy) {
                    accountStatusCopy.textContent = "个人中心暂时不可用，请稍后刷新重试。";
                }
                return;
            }

            if (accountStatusCopy) {
                accountStatusCopy.textContent = "个人中心暂时不可用，请稍后刷新重试。";
            }
            renderProfileList(accountFavoritesList, [], "个人中心暂时不可用。");
            renderProfileList(accountDownloadsList, [], "个人中心暂时不可用。");
        }
    }

    async function handleAuthSuccess(payload, shouldPersist) {
        rememberAccessToken(payload.accessToken, shouldPersist);
        authState.currentUser = {
            displayName: displayText(payload.displayName, ""),
            email: normalizeValue(payload.email)
        };

        syncAccountUi(buildAuthenticatedProfileFallback());
        restoreStaticCatalogSections();
        resetPasswordVisibility();

        if (loginForm) {
            loginForm.reset();
        }
        if (registerForm) {
            registerForm.reset();
        }

        switchAuthMode("login", { clearStatus: false });

        await Promise.all([loadCatalog(), loadProfile()]);
    }

    function promptLogin(message) {
        if (window.confirm(message || "登录后即可使用完整功能，现在去登录？")) {
            const accountSection = document.getElementById("account");
            if (accountSection) {
                accountSection.scrollIntoView({ behavior: "smooth" });
            }
            switchAuthMode("login", { focus: false });
            const loginInput = document.getElementById("loginEmail");
            if (loginInput) {
                setTimeout(function () { loginInput.focus(); }, 500);
            }
        }
    }

    async function toggleFavorite(button) {
        const wallpaperId = normalizeValue(button && button.dataset.wallpaperId);
        if (!wallpaperId) {
            return;
        }

        if (!authState.accessToken) {
            promptLogin("收藏功能需要登录，现在去登录？");
            return;
        }

        const shouldFavorite = button.dataset.favorited !== "true";
        setButtonBusy(button, true);
        if (modalFavoriteButton && modalFavoriteButton !== button && modalFavoriteButton.dataset.wallpaperId === wallpaperId) {
            setButtonBusy(modalFavoriteButton, true);
        }

        try {
            const endpoint = "/api/catalog/wallpapers/" + encodeURIComponent(wallpaperId) + "/favorite";
            const payload = shouldFavorite
                ? await requestJson(endpoint, { method: "POST" })
                : await requestJson(endpoint, { method: "DELETE" });

            setFavoriteButtonState(button, Boolean(payload.favorited));
            if (modalFavoriteButton && modalFavoriteButton.dataset.wallpaperId === wallpaperId) {
                setFavoriteButtonState(modalFavoriteButton, Boolean(payload.favorited));
            }

            await Promise.all([loadCatalog(), loadProfile()]);
        } catch (error) {
            window.alert(error.message || "收藏操作失败，请稍后重试。");
        } finally {
            setButtonBusy(button, false);
            if (modalFavoriteButton && modalFavoriteButton !== button && modalFavoriteButton.dataset.wallpaperId === wallpaperId) {
                setButtonBusy(modalFavoriteButton, false);
            }
        }
    }

    async function recordDownload(wallpaperId) {
        const normalizedWallpaperId = normalizeValue(wallpaperId);
        if (!normalizedWallpaperId) {
            return;
        }

        // 未登录时静默跳过记录，不弹窗阻断浏览器下载
        if (!authState.accessToken) {
            return;
        }

        try {
            await requestJson("/api/catalog/wallpapers/" + encodeURIComponent(normalizedWallpaperId) + "/downloads", {
                method: "POST"
            });
            loadProfile();
        } catch (error) {
            // Downloads should still continue even if analytics tracking fails.
        }
    }

    async function initializeAccountExperience() {
        ensureVisitorKey();
        await syncAuthStatus();
        await Promise.all([loadCatalog(), loadProfile()]);
    }

    function renderCatalog(overview) {
        const brands = Array.isArray(overview && overview.brands)
            ? overview.brands.map(function (brand) {
                return {
                    slug: normalizeValue(brand.slug),
                    name: brand.name,
                    displayName: brand.displayName,
                    folderName: brand.folderName,
                    wallpaperCount: brand.wallpaperCount || 0,
                    coverImageUrl: brand.coverImageUrl || "",
                    wallpapers: Array.isArray(brand.wallpapers) ? brand.wallpapers : []
                };
            }).filter(function (brand) {
                return Boolean(brand.slug);
            })
            : [];

        dynamicBrandHashes = new Set(["#brands"]);
        brands.forEach(function (brand) {
            dynamicBrandHashes.add("#" + brand.slug);
        });

        renderBrandNavigation(brands);

        // 未登录时只显示轮播图，画廊区替换为登录引导。
        if (!authState.accessToken) {
            renderGalleryLoginGate(brands.length);
        } else {
            renderBrandSections(brands);
        }

        renderCarousel(brands);
        setCatalogStatus("", "");
        highlightCurrentNav();
        updateBackToHomeButton();

        if (window.location.hash) {
            window.setTimeout(function () {
                scrollToHashTarget(window.location.hash, false);
            }, 0);
        }
    }

    function loadCatalog() {
        if (!gallerySection) {
            return Promise.resolve();
        }

        setCatalogStatus("正在从后端加载最新壁纸库…", "loading");

        return requestJson("/api/catalog")
            .then(renderCatalog)
            .catch(function () {
                if (authState.accessToken) {
                    restoreStaticCatalogSections();
                } else if (lastCatalogRenderMode === "catalog" || lastCatalogRenderMode === "gate") {
                    renderGalleryLoginGate(initialGalleryBrandCount || 12);
                } else {
                    restoreStaticCatalogSections();
                }
                setCatalogStatus("暂时无法从后端加载最新图床，当前显示静态备用内容。", "error");
                highlightCurrentNav();
                updateBackToHomeButton();
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

    document.addEventListener("click", function (event) {
        const hashLink = event.target.closest('a[href^="#"]');
        if (hashLink) {
            const href = hashLink.getAttribute("href") || "";
            if (href && href !== "#") {
                event.preventDefault();
                if (hashLink.closest("nav") && isMobileViewport()) {
                    closeMenu();
                    window.setTimeout(function () {
                        scrollToHashTarget(href, true);
                    }, 60);
                } else {
                    scrollToHashTarget(href, true);
                    if (hashLink.closest("nav")) {
                        closeMenu();
                    }
                }
                return;
            }
        }

        const favoriteButton = event.target.closest(".favorite-toggle");
        if (favoriteButton) {
            event.preventDefault();
            event.stopPropagation();
            toggleFavorite(favoriteButton);
            return;
        }

        const profileItem = event.target.closest(".profile-item");
        if (profileItem && modal && modalPlaceholder && modalHires && downloadBtn) {
            event.preventDefault();
            openModal(profileItem);
            return;
        }

        const clickedCard = event.target.closest(".image-card");
        if (clickedCard && modal && modalPlaceholder && modalHires && downloadBtn) {
            event.preventDefault();
            openModal(clickedCard);
            return;
        }

        if (!isMobileViewport() || !navList || !mobileMenu) {
            return;
        }

        const clickedInsideNav = navElement && event.target.closest("nav");
        if (!clickedInsideNav && navList.classList.contains("active")) {
            closeMenu();
        }
    });

    document.addEventListener("pointerover", function (event) {
        const card = event.target.closest(".image-card");
        if (card) {
            preloadHiRes(card);
        }
    });

    document.addEventListener("touchstart", function (event) {
        const card = event.target.closest(".image-card");
        if (card) {
            preloadHiRes(card);
        }
    }, { passive: true });

    if (modal && modalPlaceholder && modalHires && downloadBtn) {
        modal.setAttribute("aria-hidden", "true");

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

            modalHires.style.transform = "scale(" + modalScale + ")";
        });

        window.addEventListener("keydown", function (event) {
            if (event.key === "Escape" && modal.classList.contains("show")) {
                closeModal();
            }
        });

        downloadBtn.addEventListener("click", function (event) {
            event.preventDefault();
            var href = downloadBtn.href;  // 取已解析的绝对路径
            var fileName = downloadBtn.getAttribute("download") || "wallpaper.jpg";
            if (!href || href === "#") {
                return;
            }
            triggerFileDownload(href, fileName);
            recordDownload(downloadBtn.dataset.wallpaperId);
        });
    }

    if (prevBtn) {
        prevBtn.addEventListener("click", function () {
            handleManualCarousel("prev");
        });
    }

    if (nextBtn) {
        nextBtn.addEventListener("click", function () {
            handleManualCarousel("next");
        });
    }

    document.addEventListener("visibilitychange", function () {
        if (document.hidden) {
            stopCarousel();
        } else {
            startCarousel();
        }
    });

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
                    "暂时无法连接后端服务，请稍后再试。",
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

    [
        loginEmailInput,
        loginPasswordInput,
        registerDisplayNameInput,
        registerEmailInput,
        registerPasswordInput
    ].filter(Boolean).forEach(function (input) {
        input.addEventListener("input", function () {
            setInputInvalid(this, false);

            if (this === loginEmailInput || this === registerEmailInput) {
                syncAuthEmails(this);
            }
        });
    });

    if (rememberLoginCheckbox) {
        rememberLoginCheckbox.addEventListener("change", function () {
            syncRememberCheckboxes(rememberLoginCheckbox.checked);
        });
    }

    if (rememberRegisterCheckbox) {
        rememberRegisterCheckbox.addEventListener("change", function () {
            syncRememberCheckboxes(rememberRegisterCheckbox.checked);
        });
    }

    authTabButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            switchAuthMode(button.dataset.authTab, { focus: true });
        });

        button.addEventListener("keydown", function (event) {
            if (event.key === "ArrowLeft" || event.key === "ArrowUp") {
                event.preventDefault();
                moveAuthTabFocus(-1);
                return;
            }

            if (event.key === "ArrowRight" || event.key === "ArrowDown") {
                event.preventDefault();
                moveAuthTabFocus(1);
                return;
            }

            if (event.key === "Home") {
                event.preventDefault();
                switchAuthMode("login", { focus: false });
                if (authTabButtons[0]) {
                    authTabButtons[0].focus();
                }
                return;
            }

            if (event.key === "End") {
                event.preventDefault();
                switchAuthMode("register", { focus: false });
                if (authTabButtons[authTabButtons.length - 1]) {
                    authTabButtons[authTabButtons.length - 1].focus();
                }
            }
        });
    });

    authSwitchButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            switchAuthMode(button.dataset.authSwitch, { focus: true });
        });
    });

    passwordToggleButtons.forEach(function (button) {
        refreshPasswordToggleButton(button);
        button.addEventListener("click", function () {
            const targetInput = document.getElementById(button.dataset.passwordTarget || "");
            if (!targetInput) {
                return;
            }

            targetInput.type = targetInput.type === "password" ? "text" : "password";
            refreshPasswordToggleButton(button);
            targetInput.focus();
        });
    });

    switchAuthMode(currentAuthMode, { clearStatus: false, focus: false });
    syncPasswordHintState(registerPasswordInput && registerPasswordInput.value);

    if (loginForm) {
        const enhancedLoginSubmitButton = loginForm.querySelector(".form-submit");
        const enhancedLoginStatus = ensureFormStatusElement(loginForm);
        const enhancedLoginFieldMap = {
            email: loginEmailInput,
            password: loginPasswordInput
        };

        loginForm.addEventListener("submit", async function (event) {
            event.preventDefault();
            event.stopImmediatePropagation();
            setFormStatus(enhancedLoginStatus, "", "");
            applyAuthFieldErrors(null, enhancedLoginFieldMap);

            const normalizedLoginEmail = normalizeValue(loginEmailInput && loginEmailInput.value);
            const normalizedLoginPassword = loginPasswordInput ? loginPasswordInput.value : "";

            if (!normalizedLoginEmail || !normalizedLoginPassword.trim()) {
                setInputInvalid(loginEmailInput, !normalizedLoginEmail);
                setInputInvalid(loginPasswordInput, !normalizedLoginPassword.trim());
                setFormStatus(enhancedLoginStatus, "请输入邮箱和密码。", "error");
                return;
            }

            if (!isValidEmailAddress(normalizedLoginEmail)) {
                setInputInvalid(loginEmailInput, true);
                setFormStatus(enhancedLoginStatus, "请输入有效的邮箱地址。", "error");
                return;
            }

            if (normalizedLoginPassword.length < 8) {
                setInputInvalid(loginPasswordInput, true);
                setFormStatus(enhancedLoginStatus, "密码至少需要 8 位。", "error");
                return;
            }

            setButtonBusy(enhancedLoginSubmitButton, true);
            try {
                const payload = await requestJson("/api/auth/login", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        email: normalizedLoginEmail,
                        password: normalizedLoginPassword
                    })
                });

                setFormStatus(enhancedLoginStatus, "登录成功，正在同步你的收藏。", "success");
                await handleAuthSuccess(payload, rememberLoginCheckbox ? rememberLoginCheckbox.checked : true);
            } catch (error) {
                applyAuthFieldErrors(error.fieldErrors, enhancedLoginFieldMap);
                if (!error.fieldErrors) {
                    setInputInvalid(loginEmailInput, true);
                    setInputInvalid(loginPasswordInput, true);
                }
                setFormStatus(enhancedLoginStatus, getFriendlyAuthError(error.message, error.fieldErrors), "error");
            } finally {
                setButtonBusy(enhancedLoginSubmitButton, false);
            }
        }, true);
    }

    if (registerForm) {
        const enhancedRegisterSubmitButton = registerForm.querySelector(".form-submit");
        const enhancedRegisterStatus = ensureFormStatusElement(registerForm);
        const enhancedRegisterFieldMap = {
            displayName: registerDisplayNameInput,
            email: registerEmailInput,
            password: registerPasswordInput
        };

        registerForm.addEventListener("submit", async function (event) {
            event.preventDefault();
            event.stopImmediatePropagation();
            setFormStatus(enhancedRegisterStatus, "", "");
            applyAuthFieldErrors(null, enhancedRegisterFieldMap);

            const registerDisplayName = normalizeValue(registerDisplayNameInput && registerDisplayNameInput.value);
            const registerEmail = normalizeValue(registerEmailInput && registerEmailInput.value);
            const registerPassword = registerPasswordInput ? registerPasswordInput.value : "";

            if (!registerDisplayName || !registerEmail || !registerPassword.trim()) {
                setInputInvalid(registerDisplayNameInput, !registerDisplayName);
                setInputInvalid(registerEmailInput, !registerEmail);
                setInputInvalid(registerPasswordInput, !registerPassword.trim());
                setFormStatus(enhancedRegisterStatus, "请完整填写昵称、邮箱和密码。", "error");
                return;
            }

            if (!isValidEmailAddress(registerEmail)) {
                setInputInvalid(registerEmailInput, true);
                setFormStatus(enhancedRegisterStatus, "请输入有效的邮箱地址。", "error");
                return;
            }

            if (registerDisplayName.length > 120) {
                setInputInvalid(registerDisplayNameInput, true);
                setFormStatus(enhancedRegisterStatus, "昵称最多 120 个字符。", "error");
                return;
            }

            if (registerPassword.length < 8 || !/[A-Za-z]/.test(registerPassword) || !/[0-9]/.test(registerPassword)) {
                setInputInvalid(registerPasswordInput, true);
                setFormStatus(enhancedRegisterStatus, "密码需要至少 8 位，并同时包含字母和数字。", "error");
                return;
            }

            setButtonBusy(enhancedRegisterSubmitButton, true);
            try {
                const payload = await requestJson("/api/auth/register", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        displayName: registerDisplayName,
                        email: registerEmail,
                        password: registerPassword
                    })
                });

                setFormStatus(enhancedRegisterStatus, "注册成功，正在为你创建个人中心。", "success");
                await handleAuthSuccess(payload, rememberRegisterCheckbox ? rememberRegisterCheckbox.checked : true);
            } catch (error) {
                applyAuthFieldErrors(error.fieldErrors, enhancedRegisterFieldMap);
                if (!error.fieldErrors && /already registered|already exists/i.test(normalizeValue(error.message))) {
                    setInputInvalid(registerEmailInput, true);
                }
                setFormStatus(enhancedRegisterStatus, getFriendlyAuthError(error.message, error.fieldErrors), "error");
            } finally {
                setButtonBusy(enhancedRegisterSubmitButton, false);
            }
        }, true);
    }

    if (loginForm) {
        const loginSubmitButton = loginForm.querySelector(".form-submit");
        const loginStatus = ensureFormStatusElement(loginForm);
        const loginFieldMap = {
            email: loginEmailInput,
            password: loginPasswordInput
        };

        loginForm.addEventListener("submit", async function (event) {
            event.preventDefault();
            setFormStatus(loginStatus, "", "");
            applyAuthFieldErrors(null, loginFieldMap);

            const normalizedLoginEmail = loginEmailInput.value.trim();
            const normalizedLoginPassword = loginPasswordInput.value;

            if (!normalizedLoginEmail || !normalizedLoginPassword.trim()) {
                setInputInvalid(loginEmailInput, !normalizedLoginEmail);
                setInputInvalid(loginPasswordInput, !normalizedLoginPassword.trim());
                setFormStatus(loginStatus, "请输入邮箱和密码。", "error");
                return;
            }

            if (!isValidEmailAddress(normalizedLoginEmail)) {
                setInputInvalid(loginEmailInput, true);
                setFormStatus(loginStatus, "请输入有效的邮箱地址。", "error");
                return;
            }

            if (normalizedLoginPassword.length < 8) {
                setInputInvalid(loginPasswordInput, true);
                setFormStatus(loginStatus, "密码至少需要 8 位。", "error");
                return;
            }

            if (false) {
                setFormStatus(loginStatus, "请输入邮箱和密码。", "error");
                return;
            }

            setButtonBusy(loginSubmitButton, true);
            try {
                const payload = await requestJson("/api/auth/login", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        email: normalizedLoginEmail,
                        password: normalizedLoginPassword
                    })
                });

                setFormStatus(loginStatus, "登录成功，正在同步你的收藏。", "success");
                await handleAuthSuccess(payload, rememberLoginCheckbox ? rememberLoginCheckbox.checked : true);
            } catch (error) {
                applyAuthFieldErrors(error.fieldErrors, loginFieldMap);
                if (!error.fieldErrors) {
                    setInputInvalid(loginEmailInput, true);
                    setInputInvalid(loginPasswordInput, true);
                }
                setFormStatus(loginStatus, friendlyAuthError(error.message, error.fieldErrors), "error");
            } finally {
                setButtonBusy(loginSubmitButton, false);
            }
        });
    }

    if (registerForm) {
        const registerSubmitButton = registerForm.querySelector(".form-submit");
        const registerStatus = ensureFormStatusElement(registerForm);
        const registerFieldMap = {
            displayName: registerDisplayNameInput,
            email: registerEmailInput,
            password: registerPasswordInput
        };
        const hintLength = document.getElementById("hintLength");
        const hintLetter = document.getElementById("hintLetter");
        const hintNumber = document.getElementById("hintNumber");

        // 瀵嗙爜寮哄害瀹炴椂鎻愮ず
        if (registerPasswordInput) {
            registerPasswordInput.addEventListener("input", function () {
                const value = this.value;
                toggleHint(hintLength, value.length >= 8);
                toggleHint(hintLetter, /[A-Za-z]/.test(value));
                toggleHint(hintNumber, /[0-9]/.test(value));
            });
        }

        registerForm.addEventListener("submit", async function (event) {
            event.preventDefault();
            setFormStatus(registerStatus, "", "");
            applyAuthFieldErrors(null, registerFieldMap);

            const registerDisplayName = registerDisplayNameInput.value.trim();
            const registerEmail = registerEmailInput.value.trim();

            const registerPassword = registerPasswordInput.value;

            if (!registerDisplayName || !registerEmail || !registerPassword.trim()) {
                setInputInvalid(registerDisplayNameInput, !registerDisplayName);
                setInputInvalid(registerEmailInput, !registerEmail);
                setInputInvalid(registerPasswordInput, !registerPassword.trim());
                setFormStatus(registerStatus, "请完整填写昵称、邮箱和密码。", "error");
                return;
            }

            if (!isValidEmailAddress(registerEmail)) {
                setInputInvalid(registerEmailInput, true);
                setFormStatus(registerStatus, "请输入有效的邮箱地址。", "error");
                return;
            }

            if (registerDisplayName.length > 120) {
                setInputInvalid(registerDisplayNameInput, true);
                setFormStatus(registerStatus, "昵称最多 120 个字符。", "error");
                return;
            }

            if (false) {
                setFormStatus(registerStatus, "请完整填写昵称、邮箱和密码。", "error");
                return;
            }

            if (registerPassword.length < 8 || !/[A-Za-z]/.test(registerPassword) || !/[0-9]/.test(registerPassword)) {
                setInputInvalid(registerPasswordInput, true);
                setFormStatus(registerStatus, "密码需要至少 8 位，并同时包含字母和数字。", "error");
                return;
            }

            setButtonBusy(registerSubmitButton, true);
            try {
                const payload = await requestJson("/api/auth/register", {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        displayName: registerDisplayName,
                        email: registerEmail,
                        password: registerPassword
                    })
                });

                setFormStatus(registerStatus, "注册成功，正在为你创建个人中心。", "success");
                await handleAuthSuccess(payload, rememberRegisterCheckbox ? rememberRegisterCheckbox.checked : true);
            } catch (error) {
                applyAuthFieldErrors(error.fieldErrors, registerFieldMap);
                if (!error.fieldErrors && /already registered|already exists/i.test(normalizeValue(error.message))) {
                    setInputInvalid(registerEmailInput, true);
                }
                setFormStatus(registerStatus, friendlyAuthError(error.message, error.fieldErrors), "error");
            } finally {
                setButtonBusy(registerSubmitButton, false);
            }
        });
    }

    if (logoutButton) {
        logoutButton.addEventListener("click", async function () {
            setButtonBusy(logoutButton, true);
            try {
                if (authState.accessToken) {
                    await requestJson("/api/auth/logout", { method: "POST" });
                }
            } catch (error) {
                // Local logout should still finish even if the server call fails.
            } finally {
                clearAccessToken();
                resetPasswordVisibility();
                switchAuthMode("login", { clearStatus: false, focus: false });
                await Promise.all([loadCatalog(), loadProfile()]);
                setButtonBusy(logoutButton, false);
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

    if (getCarouselImages().length > 0) {
        showCarouselImage(carouselIndex);
        startCarousel();
    }

    highlightCurrentNav();
    if (window.location.hash) {
        window.setTimeout(function () {
            scrollToHashTarget(window.location.hash, false);
        }, 0);
    }
    updateBackToHomeButton();
    syncAccountUi(null);
    loadFeedbackHighlights();
    initializeAccountExperience();

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
