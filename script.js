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
    const modalImg = document.getElementById("img01");
    const images = document.querySelectorAll('.image-grid img');
    const downloadBtn = document.getElementById("downloadBtn");
    let scale = 1;

    if (modal && modalImg && downloadBtn) {
        // 关闭模态框（统一入口，避免重复代码）
        function closeModal() {
            modal.classList.remove('show');
            downloadBtn.style.display = "none";
            if (button) button.style.display = "block";
        }

        // 为每张图片添加点击事件（渐进加载：模糊缩略图 → 高清 WebP）
        images.forEach(function (image) {
            image.addEventListener('click', function () {
                requestAnimationFrame(() => {
                    modal.classList.add('show');
                    const fullSrc = this.dataset.full || this.src;
                    const previewSrc = fullSrc.replace(/\.(jpe?g|png)$/i, '.webp');
                    scale = 1;
                    modalImg.style.transform = "scale(1)";
                    downloadBtn.href = fullSrc;
                    downloadBtn.setAttribute('download', fullSrc.split('/').pop());
                    downloadBtn.style.display = "block";
                    if (button) button.style.display = "none";

                    // 先显示已缓存的缩略图（瞬间呈现），加模糊滤镜
                    modalImg.src = this.src;
                    modalImg.classList.add('blur-loading');

                    // 后台预加载高清 WebP，完成后平滑替换
                    const hiRes = new Image();
                    hiRes.onload = function () {
                        modalImg.src = previewSrc;
                        modalImg.classList.remove('blur-loading');
                    };
                    hiRes.src = previewSrc;
                });
            });
        });

        // 关闭按钮
        const closeBtn = document.getElementsByClassName("close")[0];
        if (closeBtn) {
            closeBtn.addEventListener('click', closeModal);
        }

        // 鼠标滚轮事件
        modalImg.addEventListener('wheel', function (event) {
            event.preventDefault();
            if (event.deltaY < 0) {
                scale += 0.1;
            } else {
                if (scale > 0.1) {
                    scale -= 0.1;
                }
            }
            modalImg.style.transition = 'transform 0.3s ease';
            modalImg.style.transform = `scale(${scale})`;
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
