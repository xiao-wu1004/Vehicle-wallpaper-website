export function createFeedbackHighlightsController(options) {
    const feedbackBlockquote = options && options.feedbackBlockquote;
    const buildApiUrl = options && options.buildApiUrl;
    const displayText = options && options.displayText;
    const deferredLoadState = options && options.deferredLoadState;
    let requestPromise = null;

    function render(items) {
        if (!feedbackBlockquote || !Array.isArray(items) || items.length === 0) {
            return;
        }

        feedbackBlockquote.innerHTML = "";
        items.slice(0, 2).forEach(function (item) {
            const paragraph = document.createElement("p");
            paragraph.textContent = "\"" + displayText(item.message, "这条精选反馈还没有正文。")
                + "\" - " + displayText(item.name, "匿名用户");
            feedbackBlockquote.appendChild(paragraph);
        });
    }

    return {
        load(forceRefresh) {
            if (!feedbackBlockquote || typeof buildApiUrl !== "function") {
                return Promise.resolve([]);
            }

            if (!forceRefresh && deferredLoadState && deferredLoadState.feedbackLoaded) {
                return Promise.resolve([]);
            }

            if (requestPromise) {
                return requestPromise;
            }

            requestPromise = fetch(buildApiUrl("/api/feedback/highlights"), {
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
                .then(function (items) {
                    if (deferredLoadState) {
                        deferredLoadState.feedbackLoaded = true;
                    }
                    render(items);
                    return items;
                })
                .catch(function () {
                    return [];
                })
                .finally(function () {
                    requestPromise = null;
                });

            return requestPromise;
        }
    };
}
