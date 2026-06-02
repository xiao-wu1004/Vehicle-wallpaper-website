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
    const closeBtn = document.querySelector(".close");
    const form = document.querySelector("#contact form");
    const feedbackBlockquote = document.querySelector("#feedback blockquote");
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

    let dynamicBrandHashes = new Set(["#brands"]);
    let modalScale = 1;
    let carouselIndex = 0;
    let carouselTimer = null;
    let userStoppedCarousel = false;
    let catalogStatusElement = null;

    function normalizeValue(value) {
        return value == null ? "" : String(value).trim();
    }

    function looksLikeMojibake(value) {
        return /[\u0080-\u009f脙脗芒氓盲忙莽茅猫锚毛矛铆卯茂冒帽貌贸么玫枚霉煤没眉鈧劉锟絔]/.test(value);
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
        const image = card.querySelector("img");
        if (!image) {
            return "";
        }
        return image.currentSrc || image.getAttribute("src") || "";
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
        removeScrollLock("modal");
    }

    function openModal(card) {
        if (!modal || !modalPlaceholder || !modalHires || !downloadBtn) {
            return;
        }

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
        formElement.appendChild(statusElement);
        return statusElement;
    }

    function setFormStatus(statusElement, message, type) {
        if (!statusElement) {
            return;
        }

        statusElement.textContent = message || "";
        statusElement.classList.remove("is-success", "is-error");

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
        const button = document.createElement("button");
        button.className = "image-card";
        button.type = "button";
        button.setAttribute("aria-label", "预览" + brandName + "壁纸" + (index + 1));
        button.dataset.full = resolveAssetUrl(wallpaper.fullUrl || wallpaper.downloadUrl || wallpaper.previewUrl);

        const image = document.createElement("img");
        image.src = resolveAssetUrl(wallpaper.previewUrl || wallpaper.fullUrl);
        image.alt = title;
        image.loading = "lazy";
        image.decoding = "async";

        button.appendChild(image);
        return button;
    }

    function clearDynamicCatalogSections() {
        if (!gallerySection) {
            return;
        }

        Array.prototype.slice.call(gallerySection.querySelectorAll(".brand-gallery")).forEach(function (section) {
            section.parentNode.removeChild(section);
        });
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
        renderBrandSections(brands);
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

        return fetch(buildApiUrl("/api/catalog"), {
            headers: {
                Accept: "application/json"
            }
        })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error("Failed to load catalog overview.");
                }
                return response.json();
            })
            .then(renderCatalog)
            .catch(function () {
                setCatalogStatus("暂时无法从后端加载最新图库，当前显示静态备用内容。", "error");
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
    loadFeedbackHighlights();
    loadCatalog();

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
