document.addEventListener("DOMContentLoaded", function () {
    const mobileMenu = document.getElementById("mobile-menu");
    const navList = document.getElementById("nav-list");
    const button = document.getElementById("backToHomeButton");

    // 点击汉堡菜单时切换导航显示
    mobileMenu.addEventListener("click", function () {
        navList.classList.toggle("active"); // 控制显示与隐藏
    });

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
        const currentHash = window.location.hash;
        const isHomepage = currentHash === "" || window.location.pathname === "./"; // 根据情况检查路径

        if (isHomepage) {
            button.style.display = "none"; // 如果在主页，则隐藏按钮
            button.classList.remove("show"); // 确保按钮不再显示
        } else {
            button.style.display = "block"; // 否则显示按钮
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
    const observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                button.classList.add("show"); // 当元素进入视口时，显示按钮
            } else {
                button.classList.remove("show"); // 当元素不在视口时，隐藏按钮
            }
        });
    });

    // 将按钮添加到观察列表
    observer.observe(button);

    // 监听滚动事件
    window.onscroll = function () {
        if (button.style.display === "block") { // 只有在按钮展示的情况下
            if (document.body.scrollTop > 50 || document.documentElement.scrollTop > 50) {
                button.classList.add("show"); // 超过50px时显示按钮
            } else {
                button.classList.remove("show"); // 回到顶部时隐藏按钮
            }
        }
    };

    // 获取模态框
    var modal = document.getElementById("myModal");
    var modalImg = document.getElementById("img01");
    var images = document.querySelectorAll('.image-grid img');
    const downloadBtn = document.getElementById("downloadBtn");
    var scale = 1;

    // 为每张图片添加点击事件
    images.forEach(function (image) {
        image.onclick = function () {
            requestAnimationFrame(() => {
                modal.classList.add('show'); // 添加显示模态框的类
                modalImg.src = this.src; // 设置模态框中的图像为点击的图像
                scale = 1; // 重置缩放比例
                modalImg.style.transform = "scale(1)"; // 应用初始缩放
                downloadBtn.href = this.src; // 设置下载链接
                downloadBtn.style.display = "block"; // 显示下载按钮
                // 隐藏返回主页按钮
                button.style.display = "none";
            });
        };
    });

    // 获取关闭按钮
    var span = document.getElementsByClassName("close")[0];

    // 为关闭按钮添加点击事件
    span.onclick = function () {
        modal.classList.remove('show'); // 移除模态框显示的类
        downloadBtn.style.display = "none"; // 隐藏下载按钮
        button.style.display = "block"; // 显示返回主页按钮
    };

    // 鼠标滚轮事件
    modalImg.onwheel = function (event) {
        event.preventDefault(); // 防止默认滚动行为
        if (event.deltaY < 0) { // 向上滚动（放大）
            scale += 0.1; // 增加缩放比例
        } else { // 向下滚动（缩小）
            if (scale > 0.1) { // 确保缩放比例不小于0.1
                scale -= 0.1; // 减少缩放比例
            }
        }
        // 使用 CSS 过渡实现平滑缩放
        modalImg.style.transition = 'transform 0.3s ease';
        modalImg.style.transform = `scale(${scale})`;
    };

    // 点击模态框区域也可以关闭模态框
    modal.onclick = function () {
        modal.classList.remove('show'); // 移除模态框显示的类
        downloadBtn.style.display = "none"; // 隐藏下载按钮
        button.style.display = "block"; // 显示返回主页按钮
    };

    // 按下 Esc 键关闭模态框
    window.onkeydown = function (event) {
        if (event.key === "Escape") {
            modal.classList.remove('show'); // 隐藏模态框
            downloadBtn.style.display = "none"; // 隐藏下载按钮
            button.style.display = "block"; // 显示返回主页按钮
        }
    };

    const carouselImages = document.querySelectorAll('.carousel-image');
    let currentIndex = 0;

    function showImage(index) {
        carouselImages.forEach((img, i) => {
            img.style.transition = 'opacity 0.5s ease'; // 添加过渡效果
            img.style.opacity = i === index ? 1 : 0; // 使用透明度实现淡入淡出效果
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

    // 初始化显示第一张图像
    showImage(currentIndex);

    // 每隔5秒自动切换到下一张图像
    setInterval(showNextImage, 5000);

    // 为按钮添加事件监听器
    document.getElementById('nextBtn').addEventListener('click', showNextImage);
    document.getElementById('prevBtn').addEventListener('click', showPrevImage);

    function validateForm() {
        // 获取表单元素
        const name = document.getElementById('name').value.trim();
        const email = document.getElementById('email').value.trim();
        const message = document.getElementById('message').value.trim();

        // 验证姓名
        if (name === "") {
            alert("姓名不能为空！");
            return false; // 阻止表单提交
        }

        // 验证邮箱格式
        const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailPattern.test(email)) {
            alert("请输入有效的邮箱地址！");
            return false; // 阻止表单提交
        }

        // 验证留言内容
        if (message === "") {
            alert("留言内容不能为空！");
            return false; // 阻止表单提交
        }

        // 通过验证，允许提交
        return true;
    }
});