// 获取模态框
var modal = document.getElementById("myModal");

// 获取模态框中的图片元素
var modalImg = document.getElementById("img01");

// 获取所有的图片元素
var images = document.querySelectorAll('.image-grid img');

// 初始缩放比例
var scale = 1;

// 为每张图片添加点击事件
images.forEach(function(image) {
    image.onclick = function() {
        modal.style.display = "block"; // 显示模态框
        modalImg.src = this.src; // 设置模态框中的图像为点击的图像
        scale = 1; // 重置缩放比例
        modalImg.style.transform = "scale(1)"; // 应用初始缩放
    }
});

// 获取关闭按钮
var span = document.getElementsByClassName("close")[0];

// 为关闭按钮添加点击事件
span.onclick = function() {
    modal.style.display = "none"; // 隐藏模态框
}

// 鼠标滚轮事件
modalImg.onwheel = function(event) {
    event.preventDefault(); // 防止默认滚动行为
    if (event.deltaY < 0) { // 向上滚动（放大）
        scale += 0.1; // 增加缩放比例
    } else { // 向下滚动（缩小）
        if (scale > 0.1) { // 确保缩放比例不小于0.1
            scale -= 0.1; // 减少缩放比例
        }
    }
    modalImg.style.transform = "scale(" + scale + ")"; // 应用新的缩放
}

// 点击模态框区域也可以关闭模态框
modal.onclick = function() {
    modal.style.display = "none"; // 隐藏模态框
}

// 高亮当前页面导航链接
function highlightCurrentNav() {
    var navLinks = document.querySelectorAll('nav ul li a'); // 获取所有导航链接
    var currentHash = window.location.hash; // 获取当前 hash

    navLinks.forEach(function(link) {
        // 移除所有链接的 active 类
        link.classList.remove('active');
        // 如果链接的 href 与当前 hash 匹配，则添加 active 类
        if (link.getAttribute('href') === currentHash) {
            link.classList.add('active');
        }
    });
}

// 页面加载时高亮当前链接
window.onload = highlightCurrentNav;

// hashchange 事件监控链接变化
window.onhashchange = highlightCurrentNav;
