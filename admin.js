document.addEventListener("DOMContentLoaded", function () {
    const body = document.body;
    const defaultApiBase = window.location.protocol === "file:" ? "http://localhost:8080" : "";
    const savedKeyStorageName = "vehicleWallpaperAdminApiKey";
    const savedTokenStorageName = "vehicleWallpaperAdminAccessToken";
    const savedUsernameStorageName = "vehicleWallpaperAdminUsername";
    const utf8Decoder = typeof TextDecoder === "function" ? new TextDecoder("utf-8", { fatal: true }) : null;
    const defaultLoginUsername = "admin";
    const brandLabels = {
        benz: "Mercedes-Benz",
        porsche: "Porsche",
        hongqi: "HongQi",
        xiaomi: "Xiaomi",
        bmw: "BMW",
        audi: "Audi",
        ferrari: "Ferrari",
        lamborghini: "Lamborghini",
        astonmartin: "Aston Martin",
        maserati: "Maserati",
        bugatti: "Bugatti",
        ford: "Ford"
    };

    const state = {
        apiKey: "",
        accessToken: "",
        authMode: "",
        adminUsername: "",
        loginEnabled: true,
        apiKeyEnabled: true,
        loginUsernameHint: defaultLoginUsername,
        connected: false,
        dashboard: null,
        wallpapers: [],
        feedback: [],
        wallpaperSearch: "",
        selectedWallpaperId: null,
        selectedFeedbackId: null
    };

    const elements = {
        loginForm: document.getElementById("loginForm"),
        loginUsernameInput: document.getElementById("loginUsernameInput"),
        loginPasswordInput: document.getElementById("loginPasswordInput"),
        rememberLoginCheckbox: document.getElementById("rememberLoginCheckbox"),
        loginButton: document.getElementById("loginButton"),
        logoutButton: document.getElementById("logoutButton"),
        loginSupportCopy: document.getElementById("loginSupportCopy"),
        toggleLoginPasswordButton: document.getElementById("toggleLoginPasswordButton"),
        apiKeyFallback: document.getElementById("apiKeyFallback"),
        authForm: document.getElementById("authForm"),
        apiKeyInput: document.getElementById("apiKeyInput"),
        rememberKeyCheckbox: document.getElementById("rememberKeyCheckbox"),
        connectButton: document.getElementById("connectButton"),
        clearKeyButton: document.getElementById("clearKeyButton"),
        toggleApiKeyButton: document.getElementById("toggleApiKeyButton"),
        connectionStatus: document.getElementById("connectionStatus"),
        noticeBanner: document.getElementById("noticeBanner"),
        refreshAllButton: document.getElementById("refreshAllButton"),
        refreshCatalogButton: document.getElementById("refreshCatalogButton"),
        lastSyncTime: document.getElementById("lastSyncTime"),
        wallpaperBrandFilter: document.getElementById("wallpaperBrandFilter"),
        wallpaperActiveFilter: document.getElementById("wallpaperActiveFilter"),
        wallpaperSearchInput: document.getElementById("wallpaperSearchInput"),
        wallpaperListSummary: document.getElementById("wallpaperListSummary"),
        wallpaperList: document.getElementById("wallpaperList"),
        wallpaperEditorHint: document.getElementById("wallpaperEditorHint"),
        wallpaperEditorForm: document.getElementById("wallpaperEditorForm"),
        wallpaperPreview: document.getElementById("wallpaperPreview"),
        wallpaperPreviewPlaceholder: document.getElementById("wallpaperPreviewPlaceholder"),
        wallpaperTitleInput: document.getElementById("wallpaperTitleInput"),
        wallpaperSortInput: document.getElementById("wallpaperSortInput"),
        wallpaperBrandDisplay: document.getElementById("wallpaperBrandDisplay"),
        wallpaperFileNameDisplay: document.getElementById("wallpaperFileNameDisplay"),
        wallpaperActiveInput: document.getElementById("wallpaperActiveInput"),
        wallpaperMeta: document.getElementById("wallpaperMeta"),
        saveWallpaperButton: document.getElementById("saveWallpaperButton"),
        wallpaperDownloadLink: document.getElementById("wallpaperDownloadLink"),
        feedbackStatusFilter: document.getElementById("feedbackStatusFilter"),
        feedbackFeaturedFilter: document.getElementById("feedbackFeaturedFilter"),
        feedbackListSummary: document.getElementById("feedbackListSummary"),
        feedbackList: document.getElementById("feedbackList"),
        feedbackEditorHint: document.getElementById("feedbackEditorHint"),
        feedbackEditorForm: document.getElementById("feedbackEditorForm"),
        feedbackNameDisplay: document.getElementById("feedbackNameDisplay"),
        feedbackEmailDisplay: document.getElementById("feedbackEmailDisplay"),
        feedbackStatusInput: document.getElementById("feedbackStatusInput"),
        feedbackSourceDisplay: document.getElementById("feedbackSourceDisplay"),
        feedbackMessageDisplay: document.getElementById("feedbackMessageDisplay"),
        feedbackFeaturedInput: document.getElementById("feedbackFeaturedInput"),
        feedbackMeta: document.getElementById("feedbackMeta"),
        saveFeedbackButton: document.getElementById("saveFeedbackButton"),
        approveFeedbackButton: document.getElementById("approveFeedbackButton"),
        rejectFeedbackButton: document.getElementById("rejectFeedbackButton"),
        metricTotalBrands: document.getElementById("metricTotalBrands"),
        metricTotalWallpapers: document.getElementById("metricTotalWallpapers"),
        metricActiveWallpapers: document.getElementById("metricActiveWallpapers"),
        metricInactiveWallpapers: document.getElementById("metricInactiveWallpapers"),
        metricPendingFeedback: document.getElementById("metricPendingFeedback"),
        metricApprovedFeedback: document.getElementById("metricApprovedFeedback"),
        metricRejectedFeedback: document.getElementById("metricRejectedFeedback"),
        metricFeaturedFeedback: document.getElementById("metricFeaturedFeedback")
    };

    function getApiBase() {
        const configuredApiBase = body.getAttribute("data-api-base")
            || (window.VEHICLE_WALLPAPER_CONFIG && window.VEHICLE_WALLPAPER_CONFIG.apiBase)
            || "";
        return (String(configuredApiBase).trim() || defaultApiBase).replace(/\/$/, "");
    }

    function buildApiUrl(path) {
        return getApiBase() + path;
    }

    function normalizeValue(value) {
        return value == null ? "" : String(value).trim();
    }

    function activeTokenStorage() {
        if (localStorage.getItem(savedTokenStorageName)) {
            return localStorage;
        }

        if (sessionStorage.getItem(savedTokenStorageName)) {
            return sessionStorage;
        }

        return null;
    }

    function rememberToken(token, username, shouldPersist) {
        localStorage.removeItem(savedTokenStorageName);
        sessionStorage.removeItem(savedTokenStorageName);

        const targetStorage = shouldPersist ? localStorage : sessionStorage;
        targetStorage.setItem(savedTokenStorageName, token);

        if (username) {
            localStorage.setItem(savedUsernameStorageName, username);
        }
    }

    function readRememberedToken() {
        return localStorage.getItem(savedTokenStorageName) || sessionStorage.getItem(savedTokenStorageName) || "";
    }

    function clearRememberedToken() {
        localStorage.removeItem(savedTokenStorageName);
        sessionStorage.removeItem(savedTokenStorageName);
    }

    function restoreSavedUsername() {
        const savedUsername = localStorage.getItem(savedUsernameStorageName);
        if (savedUsername) {
            elements.loginUsernameInput.value = savedUsername;
        }
    }

    function looksLikeMojibake(value) {
        return /[\u0080-\u009fÃÂâåäæçéèêëìíîïðñòóôõöùúûü€™�]/.test(value);
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

    function brandLabel(slug) {
        const normalizedSlug = normalizeValue(slug).toLowerCase();
        return brandLabels[normalizedSlug] || normalizedSlug || "Unknown brand";
    }

    function formatDateTime(value) {
        if (!value) {
            return "Not available";
        }

        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return value;
        }

        return new Intl.DateTimeFormat("en-GB", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit"
        }).format(date);
    }

    function basename(fileName) {
        const normalized = normalizeValue(fileName);
        if (!normalized) {
            return "";
        }

        return normalized.replace(/\.[^.]+$/, "");
    }

    function bestWallpaperTitle(wallpaper) {
        const title = displayText(wallpaper.title);
        if (title) {
            return title;
        }

        return displayText(basename(wallpaper.fileName), wallpaper.slug || "Untitled wallpaper");
    }

    function setNotice(message, type) {
        const normalizedMessage = normalizeValue(message);
        if (!normalizedMessage) {
            elements.noticeBanner.textContent = "";
            elements.noticeBanner.className = "notice-banner";
            return;
        }

        elements.noticeBanner.textContent = normalizedMessage;
        elements.noticeBanner.className = "notice-banner is-visible";
        elements.noticeBanner.classList.add(type === "error" ? "is-error" : type === "success" ? "is-success" : "is-info");
    }

    function setConnectionStatus(label, tone) {
        elements.connectionStatus.textContent = label;
        elements.connectionStatus.className = "status-pill";
        if (tone === "connected") {
            elements.connectionStatus.classList.add("is-connected");
        } else if (tone === "loading") {
            elements.connectionStatus.classList.add("is-loading");
        } else if (tone === "error") {
            elements.connectionStatus.classList.add("is-error");
        }
    }

    function clearDataViews() {
        state.dashboard = null;
        state.wallpapers = [];
        state.feedback = [];
        state.selectedWallpaperId = null;
        state.selectedFeedbackId = null;

        renderDashboard();
        renderWallpaperList();
        renderWallpaperEditor();
        renderFeedbackList();
        renderFeedbackEditor();
    }

    async function loadAuthOptions() {
        try {
            const response = await fetch(buildApiUrl("/api/admin/auth/options"), {
                headers: {
                    Accept: "application/json"
                }
            });
            const payload = await response.json().catch(function () {
                return {};
            });

            state.loginEnabled = Boolean(payload.loginEnabled);
            state.apiKeyEnabled = Boolean(payload.apiKeyEnabled);
            state.loginUsernameHint = normalizeValue(payload.loginUsernameHint) || defaultLoginUsername;
        } catch (error) {
            state.loginEnabled = true;
            state.apiKeyEnabled = true;
            state.loginUsernameHint = defaultLoginUsername;
        }

        if (!elements.loginUsernameInput.value) {
            elements.loginUsernameInput.value = state.loginUsernameHint;
        }

        elements.loginUsernameInput.placeholder = state.loginUsernameHint;

        if (!state.apiKeyEnabled && elements.apiKeyFallback) {
            elements.apiKeyFallback.hidden = true;
        }

        if (!state.loginEnabled) {
            elements.loginForm.querySelectorAll("input, button").forEach(function (element) {
                if (element !== elements.logoutButton) {
                    element.disabled = true;
                }
            });
            elements.loginSupportCopy.textContent = "This deployment has not enabled username/password login yet. Use the API key fallback instead.";
            if (elements.apiKeyFallback) {
                elements.apiKeyFallback.open = true;
            }
        }
    }

    async function fetchAdmin(path, options) {
        const headers = {
            Accept: "application/json"
        };
        const requestOptions = Object.assign({ method: "GET" }, options || {});

        if (state.accessToken) {
            headers.Authorization = "Bearer " + state.accessToken;
        } else if (state.apiKey) {
            headers["X-Admin-API-Key"] = state.apiKey;
        }

        if (requestOptions.body) {
            headers["Content-Type"] = "application/json";
        }

        requestOptions.headers = Object.assign(headers, requestOptions.headers || {});

        const response = await fetch(buildApiUrl(path), requestOptions);
        const payload = await response.json().catch(function () {
            return {};
        });

        if (!response.ok) {
            if (response.status === 401 && state.accessToken) {
                clearRememberedToken();
                state.accessToken = "";
                state.authMode = "";
                state.adminUsername = "";
            }

            const error = new Error(payload.message || "Admin request failed.");
            error.status = response.status;
            error.payload = payload;
            throw error;
        }

        return payload;
    }

    async function loadDashboard() {
        state.dashboard = await fetchAdmin("/api/admin/dashboard");
        renderDashboard();
    }

    async function loadWallpapers() {
        const params = new URLSearchParams();
        const brand = normalizeValue(elements.wallpaperBrandFilter.value);
        const active = normalizeValue(elements.wallpaperActiveFilter.value);

        if (brand) {
            params.set("brand", brand);
        }

        if (active) {
            params.set("active", active);
        }

        const query = params.toString();
        state.wallpapers = await fetchAdmin("/api/admin/wallpapers" + (query ? "?" + query : ""));
        renderWallpaperBrandOptions();
        renderWallpaperList();
        renderWallpaperEditor();
    }

    async function loadFeedback() {
        const params = new URLSearchParams();
        const status = normalizeValue(elements.feedbackStatusFilter.value);
        const featured = normalizeValue(elements.feedbackFeaturedFilter.value);

        if (status) {
            params.set("status", status);
        }

        if (featured) {
            params.set("featured", featured);
        }

        params.set("limit", "100");

        state.feedback = await fetchAdmin("/api/admin/feedback?" + params.toString());
        renderFeedbackList();
        renderFeedbackEditor();
    }

    async function loadAllData() {
        await Promise.all([loadDashboard(), loadWallpapers(), loadFeedback()]);
    }

    async function requestAdminLogin(username, password) {
        const response = await fetch(buildApiUrl("/api/admin/auth/login"), {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            body: JSON.stringify({
                username: normalizeValue(username),
                password: normalizeValue(password)
            })
        });
        const payload = await response.json().catch(function () {
            return {};
        });

        if (!response.ok) {
            const error = new Error(payload.message || "Admin login failed.");
            error.status = response.status;
            error.payload = payload;
            throw error;
        }

        return payload;
    }

    async function connectWithLoginToken(token, username) {
        state.apiKey = "";
        state.accessToken = normalizeValue(token);
        state.authMode = "PASSWORD";
        state.adminUsername = normalizeValue(username) || state.loginUsernameHint || defaultLoginUsername;

        if (!state.accessToken) {
            throw new Error("Missing admin access token.");
        }

        setConnectionStatus("Signing in", "loading");
        setNotice("Signing in and loading admin dashboard data.", "info");

        try {
            await loadAllData();
            state.connected = true;
            renderWallpaperList();
            renderWallpaperEditor();
            renderFeedbackList();
            renderFeedbackEditor();
            setConnectionStatus("Logged in", "connected");
            setNotice("Signed in successfully. Dashboard, catalog, and feedback data are now in sync.", "success");
        } catch (error) {
            state.connected = false;
            clearDataViews();
            setConnectionStatus("Failed", "error");
            setNotice(error.message || "Admin sign-in failed.", "error");
            throw error;
        }
    }

    async function connectWithApiKey(apiKey) {
        state.apiKey = normalizeValue(apiKey);
        state.accessToken = "";
        state.authMode = "API_KEY";
        state.adminUsername = "";

        if (!state.apiKey) {
            throw new Error("Enter a valid admin API key first.");
        }

        setConnectionStatus("Connecting", "loading");
        setNotice("Connecting to the protected admin APIs and loading dashboard data.", "info");

        try {
            await loadAllData();
            state.connected = true;
            renderWallpaperList();
            renderWallpaperEditor();
            renderFeedbackList();
            renderFeedbackEditor();
            setConnectionStatus("API key mode", "connected");
            setNotice("Admin console connected. Dashboard, catalog, and feedback data are now in sync.", "success");
        } catch (error) {
            state.connected = false;
            clearDataViews();
            setConnectionStatus("Failed", "error");
            setNotice(error.message || "Admin connection failed.", "error");
            throw error;
        }
    }

    function disconnect(resetInput) {
        state.connected = false;
        state.apiKey = "";
        state.accessToken = "";
        state.authMode = "";
        state.adminUsername = "";
        clearDataViews();
        setConnectionStatus("Disconnected", "idle");
        setNotice("Admin connection cleared.", "info");

        if (resetInput) {
            elements.apiKeyInput.value = "";
            elements.loginPasswordInput.value = "";
        }
    }

    function renderDashboard() {
        const dashboard = state.dashboard;

        elements.metricTotalBrands.textContent = dashboard ? dashboard.totalBrands : "--";
        elements.metricTotalWallpapers.textContent = dashboard ? dashboard.totalWallpapers : "--";
        elements.metricActiveWallpapers.textContent = dashboard ? dashboard.activeWallpapers : "--";
        elements.metricInactiveWallpapers.textContent = dashboard ? dashboard.inactiveWallpapers : "--";
        elements.metricPendingFeedback.textContent = dashboard ? dashboard.pendingFeedback : "--";
        elements.metricApprovedFeedback.textContent = dashboard ? dashboard.approvedFeedback : "--";
        elements.metricRejectedFeedback.textContent = dashboard ? dashboard.rejectedFeedback : "--";
        elements.metricFeaturedFeedback.textContent = dashboard ? dashboard.featuredFeedback : "--";
        elements.lastSyncTime.textContent = dashboard ? formatDateTime(dashboard.catalogGeneratedAt) : "Not loaded yet";
    }

    function renderWallpaperBrandOptions() {
        const selectedValue = elements.wallpaperBrandFilter.value;
        const slugs = [];

        state.wallpapers.forEach(function (wallpaper) {
            if (slugs.indexOf(wallpaper.brandSlug) < 0) {
                slugs.push(wallpaper.brandSlug);
            }
        });

        slugs.sort();
        elements.wallpaperBrandFilter.innerHTML = '<option value="">All brands</option>';

        slugs.forEach(function (slug) {
            const option = document.createElement("option");
            option.value = slug;
            option.textContent = brandLabel(slug);
            elements.wallpaperBrandFilter.appendChild(option);
        });

        if (selectedValue && slugs.indexOf(selectedValue) >= 0) {
            elements.wallpaperBrandFilter.value = selectedValue;
        }
    }

    function filteredWallpapers() {
        const query = normalizeValue(state.wallpaperSearch).toLowerCase();

        return state.wallpapers.filter(function (wallpaper) {
            if (!query) {
                return true;
            }

            return [
                bestWallpaperTitle(wallpaper),
                displayText(wallpaper.fileName),
                wallpaper.slug,
                brandLabel(wallpaper.brandSlug)
            ].join(" ").toLowerCase().indexOf(query) >= 0;
        });
    }

    function selectedWallpaper() {
        return state.wallpapers.find(function (wallpaper) {
            return wallpaper.id === state.selectedWallpaperId;
        }) || null;
    }

    function renderWallpaperList() {
        const wallpapers = filteredWallpapers();
        elements.wallpaperList.innerHTML = "";
        elements.wallpaperListSummary.textContent = state.connected
            ? wallpapers.length + " wallpaper entries"
            : "Connect to load data";

        if (!state.connected) {
            elements.wallpaperList.innerHTML = '<div class="entity-empty">Connect to the admin APIs to load editable wallpaper records.</div>';
            return;
        }

        if (wallpapers.length === 0) {
            elements.wallpaperList.innerHTML = '<div class="entity-empty">No wallpapers match the current filters.</div>';
            return;
        }

        wallpapers.forEach(function (wallpaper) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "entity-row" + (state.selectedWallpaperId === wallpaper.id ? " is-selected" : "");
            button.dataset.id = String(wallpaper.id);
            button.innerHTML =
                '<img class="entity-thumbnail" src="' + wallpaper.previewUrl + '" alt="">' +
                '<div class="entity-body">' +
                '<div class="entity-title">' + escapeHtml(bestWallpaperTitle(wallpaper)) + '</div>' +
                '<div class="entity-subtitle">' + escapeHtml(brandLabel(wallpaper.brandSlug)) + " / " + escapeHtml(displayText(wallpaper.fileName)) + '</div>' +
                '<div class="entity-meta">' + escapeHtml(wallpaper.slug) + '</div>' +
                '</div>' +
                '<div class="entity-side">' +
                '<span class="badge ' + (wallpaper.active ? "is-active" : "is-inactive") + '">' + (wallpaper.active ? "Active" : "Inactive") + '</span>' +
                '<span class="badge is-brand">Sort ' + wallpaper.sortOrder + '</span>' +
                '</div>';

            button.addEventListener("click", function () {
                state.selectedWallpaperId = wallpaper.id;
                renderWallpaperList();
                renderWallpaperEditor();
            });

            elements.wallpaperList.appendChild(button);
        });
    }

    function setWallpaperEditorDisabled(disabled) {
        [
            elements.wallpaperTitleInput,
            elements.wallpaperSortInput,
            elements.wallpaperBrandDisplay,
            elements.wallpaperFileNameDisplay,
            elements.wallpaperActiveInput,
            elements.saveWallpaperButton
        ].forEach(function (element) {
            element.disabled = disabled;
        });
    }

    function renderWallpaperEditor() {
        const wallpaper = selectedWallpaper();

        if (!wallpaper) {
            setWallpaperEditorDisabled(true);
            elements.wallpaperEditorHint.textContent = state.connected
                ? "Select one wallpaper from the list to edit its metadata."
                : "Connect first, then select one wallpaper from the list to edit its metadata.";
            elements.wallpaperPreview.hidden = true;
            elements.wallpaperPreview.removeAttribute("src");
            elements.wallpaperPreviewPlaceholder.hidden = false;
            elements.wallpaperTitleInput.value = "";
            elements.wallpaperSortInput.value = "";
            elements.wallpaperBrandDisplay.value = "";
            elements.wallpaperFileNameDisplay.value = "";
            elements.wallpaperActiveInput.checked = false;
            elements.wallpaperMeta.textContent = "Created time, updated time, and slug details will appear here.";
            elements.wallpaperDownloadLink.hidden = true;
            return;
        }

        setWallpaperEditorDisabled(false);
        elements.wallpaperEditorHint.textContent = "Editing wallpaper ID #" + wallpaper.id + ".";
        elements.wallpaperPreview.hidden = false;
        elements.wallpaperPreview.src = wallpaper.previewUrl;
        elements.wallpaperPreview.alt = bestWallpaperTitle(wallpaper);
        elements.wallpaperPreviewPlaceholder.hidden = true;
        elements.wallpaperTitleInput.value = bestWallpaperTitle(wallpaper);
        elements.wallpaperSortInput.value = wallpaper.sortOrder;
        elements.wallpaperBrandDisplay.value = brandLabel(wallpaper.brandSlug);
        elements.wallpaperFileNameDisplay.value = displayText(wallpaper.fileName);
        elements.wallpaperActiveInput.checked = Boolean(wallpaper.active);
        elements.wallpaperMeta.textContent =
            "Slug: " + wallpaper.slug + " / Created: " + formatDateTime(wallpaper.createdAt) + " / Updated: " + formatDateTime(wallpaper.updatedAt);
        elements.wallpaperDownloadLink.hidden = false;
        elements.wallpaperDownloadLink.href = wallpaper.fullUrl;
    }

    function selectedFeedback() {
        return state.feedback.find(function (item) {
            return item.id === state.selectedFeedbackId;
        }) || null;
    }

    function renderFeedbackList() {
        elements.feedbackList.innerHTML = "";
        elements.feedbackListSummary.textContent = state.connected
            ? state.feedback.length + " feedback entries"
            : "Connect to load data";

        if (!state.connected) {
            elements.feedbackList.innerHTML = '<div class="entity-empty">Connect to the admin APIs to review feedback submissions.</div>';
            return;
        }

        if (state.feedback.length === 0) {
            elements.feedbackList.innerHTML = '<div class="entity-empty">No feedback entries match the current filters.</div>';
            return;
        }

        state.feedback.forEach(function (item) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "entity-row" + (state.selectedFeedbackId === item.id ? " is-selected" : "");
            button.dataset.id = String(item.id);
            button.innerHTML =
                '<div class="entity-body">' +
                '<div class="entity-title">' + escapeHtml(displayText(item.name, "Anonymous")) + " / " + escapeHtml(displayText(item.email, "No email")) + '</div>' +
                '<div class="entity-subtitle">' + escapeHtml(item.sourcePage || "/unknown") + " / " + escapeHtml(formatDateTime(item.createdAt)) + '</div>' +
                '<div class="feedback-message">' + escapeHtml(displayText(item.message, "No message body.")) + '</div>' +
                '</div>' +
                '<div class="entity-side">' +
                '<span class="badge ' + feedbackStatusClass(item.status) + '">' + feedbackStatusLabel(item.status) + '</span>' +
                (item.featured ? '<span class="badge is-featured">Featured</span>' : "") +
                '</div>';

            button.addEventListener("click", function () {
                state.selectedFeedbackId = item.id;
                renderFeedbackList();
                renderFeedbackEditor();
            });

            elements.feedbackList.appendChild(button);
        });
    }

    function setFeedbackEditorDisabled(disabled) {
        [
            elements.feedbackNameDisplay,
            elements.feedbackEmailDisplay,
            elements.feedbackStatusInput,
            elements.feedbackSourceDisplay,
            elements.feedbackMessageDisplay,
            elements.feedbackFeaturedInput,
            elements.saveFeedbackButton,
            elements.approveFeedbackButton,
            elements.rejectFeedbackButton
        ].forEach(function (element) {
            element.disabled = disabled;
        });
    }

    function renderFeedbackEditor() {
        const item = selectedFeedback();

        if (!item) {
            setFeedbackEditorDisabled(true);
            elements.feedbackEditorHint.textContent = state.connected
                ? "Select one feedback submission to update its status or featured flag."
                : "Connect first, then select one feedback submission to update its status or featured flag.";
            elements.feedbackNameDisplay.value = "";
            elements.feedbackEmailDisplay.value = "";
            elements.feedbackStatusInput.value = "PENDING";
            elements.feedbackSourceDisplay.value = "";
            elements.feedbackMessageDisplay.value = "";
            elements.feedbackFeaturedInput.checked = false;
            elements.feedbackMeta.textContent = "Source page, submitted time, and user agent details will appear here.";
            return;
        }

        setFeedbackEditorDisabled(false);
        elements.feedbackEditorHint.textContent = "Reviewing feedback ID #" + item.id + ".";
        elements.feedbackNameDisplay.value = displayText(item.name, "Anonymous");
        elements.feedbackEmailDisplay.value = displayText(item.email, "No email");
        elements.feedbackStatusInput.value = item.status;
        elements.feedbackSourceDisplay.value = item.sourcePage || "/unknown";
        elements.feedbackMessageDisplay.value = displayText(item.message, "No message body.");
        elements.feedbackFeaturedInput.checked = Boolean(item.featured);
        elements.feedbackMeta.textContent =
            "Submitted: " + formatDateTime(item.createdAt) + " / User-Agent: " + (item.userAgent || "unknown");
    }

    function feedbackStatusClass(status) {
        if (status === "APPROVED") {
            return "is-approved";
        }
        if (status === "REJECTED") {
            return "is-rejected";
        }
        return "is-pending";
    }

    function feedbackStatusLabel(status) {
        if (status === "APPROVED") {
            return "Approved";
        }
        if (status === "REJECTED") {
            return "Rejected";
        }
        return "Pending";
    }

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    async function handleLoginSubmit(event) {
        event.preventDefault();

        try {
            const payload = await requestAdminLogin(elements.loginUsernameInput.value, elements.loginPasswordInput.value);
            elements.loginPasswordInput.value = "";

            if (elements.rememberLoginCheckbox.checked) {
                rememberToken(payload.accessToken, payload.username, true);
            } else {
                rememberToken(payload.accessToken, payload.username, false);
            }

            await connectWithLoginToken(payload.accessToken, payload.username);
            localStorage.setItem(savedUsernameStorageName, payload.username);
        } catch (error) {
            // The error is already shown in the notice banner.
        }
    }

    async function handleConnectSubmit(event) {
        event.preventDefault();

        try {
            await connectWithApiKey(elements.apiKeyInput.value);

            if (elements.rememberKeyCheckbox.checked) {
                localStorage.setItem(savedKeyStorageName, state.apiKey);
            } else {
                localStorage.removeItem(savedKeyStorageName);
            }
        } catch (error) {
            // The error is already shown in the notice banner.
        }
    }

    function handleLogout() {
        clearRememberedToken();
        disconnect(true);
    }

    function handleClearKey() {
        localStorage.removeItem(savedKeyStorageName);
        elements.rememberKeyCheckbox.checked = false;
        elements.apiKeyInput.value = "";

        if (state.authMode === "API_KEY") {
            disconnect(false);
        }
    }

    async function handleRefreshAll() {
        if (!state.connected) {
            setNotice("Sign in or connect with a valid fallback credential before refreshing data.", "error");
            return;
        }

        setNotice("Refreshing dashboard, catalog, and feedback data.", "info");

        try {
            await loadAllData();
            setNotice("All admin data has been refreshed.", "success");
        } catch (error) {
            setNotice(error.message || "Failed to refresh admin data.", "error");
        }
    }

    async function handleRefreshCatalog() {
        if (!state.connected) {
            setNotice("Sign in first, then trigger a catalog sync.", "error");
            return;
        }

        elements.refreshCatalogButton.disabled = true;
        setNotice("Triggering backend catalog sync.", "info");

        try {
            state.dashboard = await fetchAdmin("/api/admin/catalog/refresh", { method: "POST" });
            renderDashboard();
            await loadWallpapers();
            setNotice("Catalog sync completed and wallpaper data has been reloaded.", "success");
        } catch (error) {
            setNotice(error.message || "Catalog sync failed.", "error");
        } finally {
            elements.refreshCatalogButton.disabled = false;
        }
    }

    async function handleWallpaperSave(event) {
        event.preventDefault();

        const wallpaper = selectedWallpaper();
        if (!wallpaper) {
            return;
        }

        const payload = {
            title: normalizeValue(elements.wallpaperTitleInput.value),
            sortOrder: Number(elements.wallpaperSortInput.value || wallpaper.sortOrder),
            active: Boolean(elements.wallpaperActiveInput.checked)
        };

        elements.saveWallpaperButton.disabled = true;
        setNotice("Saving wallpaper changes.", "info");

        try {
            const updated = await fetchAdmin("/api/admin/wallpapers/" + wallpaper.id, {
                method: "PATCH",
                body: JSON.stringify(payload)
            });
            replaceWallpaper(updated);
            renderWallpaperList();
            renderWallpaperEditor();
            await loadDashboard();
            setNotice("Wallpaper changes saved.", "success");
        } catch (error) {
            setNotice(error.message || "Failed to save wallpaper changes.", "error");
        } finally {
            elements.saveWallpaperButton.disabled = false;
        }
    }

    function replaceWallpaper(updated) {
        state.wallpapers = state.wallpapers.map(function (wallpaper) {
            return wallpaper.id === updated.id ? updated : wallpaper;
        });
        state.selectedWallpaperId = updated.id;
    }

    async function handleFeedbackSave(event) {
        event.preventDefault();
        await saveFeedbackUpdate({
            status: elements.feedbackStatusInput.value,
            featured: Boolean(elements.feedbackFeaturedInput.checked)
        }, "Feedback review saved.");
    }

    async function saveFeedbackUpdate(payload, successMessage) {
        const feedback = selectedFeedback();
        if (!feedback) {
            return;
        }

        disableFeedbackActions(true);
        setNotice("Saving feedback review changes.", "info");

        try {
            const updated = await fetchAdmin("/api/admin/feedback/" + feedback.id, {
                method: "PATCH",
                body: JSON.stringify(payload)
            });
            replaceFeedback(updated);
            renderFeedbackList();
            renderFeedbackEditor();
            await loadDashboard();
            setNotice(successMessage, "success");
        } catch (error) {
            setNotice(error.message || "Failed to save feedback review.", "error");
        } finally {
            disableFeedbackActions(false);
        }
    }

    function disableFeedbackActions(disabled) {
        [
            elements.saveFeedbackButton,
            elements.approveFeedbackButton,
            elements.rejectFeedbackButton,
            elements.feedbackStatusInput,
            elements.feedbackFeaturedInput
        ].forEach(function (element) {
            element.disabled = disabled || !selectedFeedback();
        });
    }

    function replaceFeedback(updated) {
        state.feedback = state.feedback.map(function (item) {
            return item.id === updated.id ? updated : item;
        });
        state.selectedFeedbackId = updated.id;
    }

    function bindEvents() {
        elements.loginForm.addEventListener("submit", handleLoginSubmit);
        elements.authForm.addEventListener("submit", handleConnectSubmit);
        elements.logoutButton.addEventListener("click", handleLogout);
        elements.clearKeyButton.addEventListener("click", handleClearKey);
        elements.refreshAllButton.addEventListener("click", handleRefreshAll);
        elements.refreshCatalogButton.addEventListener("click", handleRefreshCatalog);
        elements.wallpaperEditorForm.addEventListener("submit", handleWallpaperSave);
        elements.feedbackEditorForm.addEventListener("submit", handleFeedbackSave);

        elements.wallpaperBrandFilter.addEventListener("change", function () {
            if (state.connected) {
                loadWallpapers().catch(function (error) {
                    setNotice(error.message || "Failed to load wallpaper list.", "error");
                });
            }
        });

        elements.wallpaperActiveFilter.addEventListener("change", function () {
            if (state.connected) {
                loadWallpapers().catch(function (error) {
                    setNotice(error.message || "Failed to load wallpaper list.", "error");
                });
            }
        });

        elements.wallpaperSearchInput.addEventListener("input", function () {
            state.wallpaperSearch = elements.wallpaperSearchInput.value;
            renderWallpaperList();
        });

        elements.feedbackStatusFilter.addEventListener("change", function () {
            if (state.connected) {
                loadFeedback().catch(function (error) {
                    setNotice(error.message || "Failed to load feedback list.", "error");
                });
            }
        });

        elements.feedbackFeaturedFilter.addEventListener("change", function () {
            if (state.connected) {
                loadFeedback().catch(function (error) {
                    setNotice(error.message || "Failed to load feedback list.", "error");
                });
            }
        });

        elements.approveFeedbackButton.addEventListener("click", function () {
            saveFeedbackUpdate({
                status: "APPROVED",
                featured: Boolean(elements.feedbackFeaturedInput.checked)
            }, "Feedback marked as approved.");
        });

        elements.rejectFeedbackButton.addEventListener("click", function () {
            saveFeedbackUpdate({
                status: "REJECTED",
                featured: Boolean(elements.feedbackFeaturedInput.checked)
            }, "Feedback marked as rejected.");
        });

        elements.toggleApiKeyButton.addEventListener("click", function () {
            const shouldReveal = elements.apiKeyInput.type === "password";
            elements.apiKeyInput.type = shouldReveal ? "text" : "password";
            elements.toggleApiKeyButton.setAttribute("aria-label", shouldReveal ? "Hide API key" : "Show API key");
            elements.toggleApiKeyButton.innerHTML = shouldReveal
                ? '<i class="fas fa-eye-slash" aria-hidden="true"></i>'
                : '<i class="fas fa-eye" aria-hidden="true"></i>';
        });

        elements.toggleLoginPasswordButton.addEventListener("click", function () {
            const shouldReveal = elements.loginPasswordInput.type === "password";
            elements.loginPasswordInput.type = shouldReveal ? "text" : "password";
            elements.toggleLoginPasswordButton.setAttribute("aria-label", shouldReveal ? "Hide password" : "Show password");
            elements.toggleLoginPasswordButton.innerHTML = shouldReveal
                ? '<i class="fas fa-eye-slash" aria-hidden="true"></i>'
                : '<i class="fas fa-eye" aria-hidden="true"></i>';
        });
    }

    async function bootstrapSavedAuth() {
        restoreSavedUsername();
        await loadAuthOptions();

        const savedAccessToken = readRememberedToken();
        if (savedAccessToken) {
            try {
                await connectWithLoginToken(savedAccessToken, localStorage.getItem(savedUsernameStorageName));
                if (activeTokenStorage() === localStorage) {
                    elements.rememberLoginCheckbox.checked = true;
                }
                return;
            } catch (error) {
                clearRememberedToken();
            }
        }

        const savedApiKey = localStorage.getItem(savedKeyStorageName);
        if (!savedApiKey) {
            setConnectionStatus("Disconnected", "idle");
            setNotice(state.loginEnabled
                ? "Sign in with your admin username and password to load the protected console."
                : "Enter an admin API key to load the protected console.", "info");
            renderDashboard();
            renderWallpaperList();
            renderWallpaperEditor();
            renderFeedbackList();
            renderFeedbackEditor();
            return;
        }

        elements.apiKeyInput.value = savedApiKey;
        elements.rememberKeyCheckbox.checked = true;

        try {
            await connectWithApiKey(savedApiKey);
        } catch (error) {
            // The error is already shown in the notice banner.
        }
    }

    bindEvents();
    bootstrapSavedAuth();
});
