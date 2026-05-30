document.addEventListener("DOMContentLoaded", function () {
    const mobileMenu = document.getElementById("mobile-menu");
    const navList = document.getElementById("nav-list");
    const button = document.getElementById("backToHomeButton");

    // 点击汉堡菜单时切换导航显示
    if (mobileMenu && navList) {
        mobileMenu.addEventListener("click", function () {
            navList.classList.toggle("active");
        });
    }

    // 高亮当前活动链接
    const navLinks = document.querySelectorAll('nav ul li a');

    function highlightCurrentNav() {
        const currentHash = window.location.hash;
        navLinks.forEach(function (link) {
            link.classList.remove('active');
            if (link.getAttribute('href') === currentHash) {
                link.classList.add('active');
            }
        });
    }

    // 检查主页路径并处理返回按钮显示状态
    function updateBackToHomeButton() {
        if (!button) return;
        const currentHash = window.location.hash;
        const isHomepage = currentHash === "" || window.location.pathname === "./";

        if (isHomepage) {
            button.style.display = "none";
            button.classList.remove("show");
        } else {
            button.style.display = "block";
        }
    }

    // 页面加载时高亮当前链接
    highlightCurrentNav();
    updateBackToHomeButton();

    // 监听 hashchange 事件，更新导航和按钮状态
    window.addEventListener('hashchange', function () {
        highlightCurrentNav();
        updateBackToHomeButton();
    });

    // 使用 Intersection Observer 来增强灵敏性
    if (button) {
        const observer = new IntersectionObserver(entries => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    button.classList.add("show");
                } else {
                    button.classList.remove("show");
                }
            });
        });
        observer.observe(button);

        // 监听滚动事件（改用 addEventListener 避免覆盖）
        window.addEventListener('scroll', function () {
            if (button.style.display === "block") {
                if (document.body.scrollTop > 50 || document.documentElement.scrollTop > 50) {
                    button.classList.add("show");
                } else {
                    button.classList.remove("show");
                }
            }
        });
    }

    // ===== 模态框（仅在主页存在） =====
    const modal = document.getElementById("myModal");
    const modalPlaceholder = document.getElementById("modalPlaceholder");
    const modalHires = document.getElementById("modalHires");
    const images = document.querySelectorAll('.image-grid img');
    const downloadBtn = document.getElementById("downloadBtn");
    let scale = 1;

    if (modal && modalPlaceholder && modalHires && downloadBtn) {
        // 关闭模态框（统一入口）
        function closeModal() {
            modal.classList.remove('show');
            modalHires.classList.remove('loaded');
            modalHires.src = '';
            modalPlaceholder.src = '';
            downloadBtn.style.display = "none";
            if (button) button.style.display = "block";
        }

        // 为每张图片：悬停预加载 + 点击渐进显示（双图层交叉淡入）
        const preloadCache = {};
        images.forEach(function (image) {
            function preloadHiRes() {
                const fullSrc = image.dataset.full || image.src;
                const previewSrc = fullSrc.replace(/\.(jpe?g|png)$/i, '.webp');
                if (!preloadCache[previewSrc]) {
                    const img = new Image();
                    img.src = previewSrc;
                    preloadCache[previewSrc] = img;
                }
            }
            image.addEventListener('mouseenter', preloadHiRes);
            image.addEventListener('touchstart', preloadHiRes, { passive: true });

            image.addEventListener('click', function () {
                requestAnimationFrame(() => {
                    modal.classList.add('show');
                    const fullSrc = this.dataset.full || this.src;
                    const previewSrc = fullSrc.replace(/\.(jpe?g|png)$/i, '.webp');
                    scale = 1;
                    modalHires.style.transform = "scale(1)";
                    modalHires.classList.remove('loaded');
                    downloadBtn.href = fullSrc;
                    downloadBtn.setAttribute('download', fullSrc.split('/').pop());
                    downloadBtn.style.display = "block";
                    if (button) button.style.display = "none";

                    // 底层：立即显示已缓存的缩略图（CSS blur 滤镜常驻）
                    modalPlaceholder.src = this.src;

                    const cached = preloadCache[previewSrc];
                    function showHiRes() {
                        modalHires.src = previewSrc;
                        // 等浏览器解码完成后再淡入，避免闪烁
                        requestAnimationFrame(() => {
                            requestAnimationFrame(() => {
                                modalHires.classList.add('loaded');
                            });
                        });
                    }

                    if (cached && cached.complete) {
                        // 已预加载 → 直接显示高清
                        showHiRes();
                    } else {
                        // 后台加载
                        const hiRes = cached || new Image();
                        hiRes.onload = showHiRes;
                        if (!cached) {
                            hiRes.src = previewSrc;
                            preloadCache[previewSrc] = hiRes;
                        }
                    }
                });
            });
        });

        // 关闭按钮
        const closeBtn = document.getElementsByClassName("close")[0];
        if (closeBtn) {
            closeBtn.addEventListener('click', closeModal);
        }

        // 鼠标滚轮缩放（作用于高清图层）
        modalHires.addEventListener('wheel', function (event) {
            event.preventDefault();
            if (event.deltaY < 0) {
                scale += 0.1;
            } else {
                if (scale > 0.1) {
                    scale -= 0.1;
                }
            }
            modalHires.style.transition = 'transform 0.3s ease';
            modalHires.style.transform = `scale(${scale})`;
        });

        // 点击模态框遮罩区域关闭（仅背景，不响应子元素点击）
        modal.addEventListener('click', function (event) {
            if (event.target === modal) {
                closeModal();
            }
        });

        // 按下 Esc 键关闭模态框（改用 addEventListener 避免覆盖）
        window.addEventListener('keydown', function (event) {
            if (event.key === "Escape") {
                closeModal();
            }
        });
    }

    // ===== 轮播图（仅在主页存在） =====
    const carouselImages = document.querySelectorAll('.carousel-image');
    if (carouselImages.length > 0) {
        let currentIndex = 0;
        let carouselTimer = null;

        function showImage(index) {
            carouselImages.forEach((img, i) => {
                img.style.transition = 'opacity 0.5s ease';
                img.style.opacity = i === index ? 1 : 0;
            });
        }

        function showNextImage() {
            currentIndex = (currentIndex + 1) % carouselImages.length;
            showImage(currentIndex);
        }

        function showPrevImage() {
            currentIndex = (currentIndex - 1 + carouselImages.length) % carouselImages.length;
            showImage(currentIndex);
        }

        function startCarousel() {
            stopCarousel();
            carouselTimer = setInterval(showNextImage, 5000);
        }

        function stopCarousel() {
            if (carouselTimer) {
                clearInterval(carouselTimer);
                carouselTimer = null;
            }
        }

        // 初始化显示第一张图像
        showImage(currentIndex);
        startCarousel();

        // 页面不可见时暂停轮播，切回时恢复
        document.addEventListener('visibilitychange', function () {
            if (document.hidden) {
                stopCarousel();
            } else {
                startCarousel();
            }
        });

        // 为按钮添加事件监听器
        const nextBtn = document.getElementById('nextBtn');
        const prevBtn = document.getElementById('prevBtn');
        if (nextBtn) nextBtn.addEventListener('click', showNextImage);
        if (prevBtn) prevBtn.addEventListener('click', showPrevImage);
    }

    // ===== 表单验证（仅在主页存在） =====
    const form = document.querySelector('#contact form');
    if (form) {
        form.addEventListener('submit', function (event) {
            if (!validateForm()) {
                event.preventDefault();
            }
        });
    }

    function validateForm() {
        const nameEl = document.getElementById('name');
        const emailEl = document.getElementById('email');
        const messageEl = document.getElementById('message');
        if (!nameEl || !emailEl || !messageEl) return true; // 无表单元素时放行

        const name = nameEl.value.trim();
        const email = emailEl.value.trim();
        const message = messageEl.value.trim();

        if (name === "") {
            alert("姓名不能为空！");
            return false;
        }

        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(email)) {
            alert("请输入有效的邮箱地址！");
            return false;
        }

        if (message === "") {
            alert("留言内容不能为空！");
            return false;
        }

        return true;
    }
});
