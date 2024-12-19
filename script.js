// 轮播图相关变量
let currentIndex = 0; // 当前显示的图片索引
const images = document.querySelectorAll('.carousel-image'); // 获取所有轮播图的图片
const totalImages = images.length;

// 显示当前图片
function showImage(index) {
    images.forEach((img, i) => {
        img.classList.remove('active');
        if (i === index) {
            img.classList.add('active');
        }
    });
}

// 下一张图片
function nextImage() {
    currentIndex = (currentIndex + 1) % totalImages; // 循环索引
    showImage(currentIndex);
}

// 上一张图片
function prevImage() {
    currentIndex = (currentIndex - 1 + totalImages) % totalImages; // 循环索引
    showImage(currentIndex);
}

// 绑定按钮事件
document.getElementById('nextBtn').addEventListener('click', nextImage);
document.getElementById('prevBtn').addEventListener('click', prevImage);

// 自动轮播功能（可选）：每隔3秒切换到下一张
setInterval(nextImage, 3000);

// 汉堡菜单功能
const mobileMenu = document.getElementById('mobile-menu');
const navList = document.getElementById('nav-list');

mobileMenu.addEventListener('click', () => {
    navList.classList.toggle('active'); // 切换菜单的显示状态
});

// 返回首页按钮的显示/隐藏效果
const backToHomeButton = document.getElementById('backToHomeButton');
const threshold = 300; // 向下滚动的阈值

window.addEventListener('scroll', () => {
    if (document.body.scrollTop > threshold || document.documentElement.scrollTop > threshold) {
        backToHomeButton.classList.add('show'); // 显示按钮
    } else {
        backToHomeButton.classList.remove('show'); // 隐藏按钮
    }
});

// 反馈表单的提交（可选）：根据实际需求进行接口调用
document.querySelector('form').addEventListener('submit', function(event) {
    event.preventDefault(); // 防止默认提交行为
    // 这里可以添加 AJAX 提交数据的代码
    alert('感谢您的反馈！'); // 提交成功提示
});

// 页面加载完成后，自动显示第一张图片
showImage(currentIndex);