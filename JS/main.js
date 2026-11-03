
"use strict";

const TIVAREK = (() => {
    const categories = {
        tivi: "Tivi",
        laptop: "Laptop",
        desktop: "Máy tính bàn",
        player: "Đầu đĩa",
        accessory: "Phụ kiện"
    };

    const icons = {
        tivi: "📺",
        laptop: "💻",
        desktop: "🖥️",
        player: "📀",
        accessory: "🔌"
    };

    function formatPrice(price) {
        return new Intl.NumberFormat("vi-VN", {
            style: "currency",
            currency: "VND"
        }).format(price);
    }

    // Mã hóa nội dung trước khi đưa vào HTML
    function escapeHTML(value) {
        return String(value ?? "").replace(/[&<>"']/g, char => {
            const map = {
                "&": "&amp;",
                "<": "&lt;",
                ">": "&gt;",
                '"': "&quot;",
                "'": "&#39;"
            };
            return map[char];
        });
    }

    // HTTP GET: đọc dữ liệu từ file JSON
    async function loadProducts() {
        const response = await fetch("DATA/products.json");

        if (!response.ok) {
            throw new Error("Không tải được dữ liệu sản phẩm.");
        }

        const products = await response.json();

        if (!Array.isArray(products)) {
            throw new Error("Dữ liệu JSON không hợp lệ.");
        }

        return products;
    }

    function productCard(product) {
        const condition = product.condition === "new"
            ? "Mới"
            : "Đã qua sử dụng";

        const stock = product.stock > 0
            ? "Còn " + product.stock + " sản phẩm"
            : "Hết hàng (demo)";

        const category = categories[product.category] || "Khác";
        const icon = icons[product.category] || "📦";

        return `
            <article class="product-card">
                <div class="product-visual">
                    <span aria-hidden="true">${icon}</span>
                    <small class="product-badge">${condition}</small>
                </div>

                <div class="product-info">
                    <p class="product-category">${category}</p>

                    <h3>${escapeHTML(product.name)}</h3>

                    <p class="product-price">
                        ${formatPrice(product.price)}
                    </p>

                    <div class="product-meta">
                        <span>${escapeHTML(stock)}</span>
                        <span>BH ${Number(product.warrantyMonths)} tháng</span>
                    </div>

                    <a class="btn"
                       href="products.html?item=${encodeURIComponent(product.id)}"
                       data-view="${escapeHTML(product.id)}">
                        Xem chi tiết →
                    </a>
                </div>
            </article>
        `;
    }

    function initMenu() {
        const button = document.querySelector(".menu-toggle");
        const nav = document.querySelector(".main-nav");

        if (!button || !nav) return;

        function closeMenu() {
            nav.classList.remove("open");
            button.setAttribute("aria-expanded", "false");
            button.setAttribute("aria-label", "Mở menu");
        }

        button.addEventListener("click", () => {
            const open = nav.classList.toggle("open");
            button.setAttribute("aria-expanded", String(open));
            button.setAttribute(
                "aria-label",
                open ? "Đóng menu" : "Mở menu"
            );
        });

        nav.querySelectorAll("a").forEach(link => {
            link.addEventListener("click", closeMenu);
        });

        document.addEventListener("keydown", event => {
            if (event.key === "Escape") closeMenu();
        });
    }

    async function renderFeatured() {
        const container = document.getElementById("featured-products");
        if (!container) return;

        try {
            const products = await loadProducts();

            const featured = products
                .filter(product => product.featured)
                .slice(0, 4);

            if (featured.length === 0) {
                container.textContent = "Chưa có sản phẩm nổi bật.";
                return;
            }

            container.innerHTML = featured.map(productCard).join("");
        } catch (error) {
            console.error(error);

            container.textContent =
                "Không thể tải sản phẩm. Hãy chạy website bằng local server.";
        }
    }

    document.addEventListener("DOMContentLoaded", () => {
        initMenu();
        renderFeatured();

        document.querySelectorAll("[data-year]").forEach(element => {
            element.textContent = new Date().getFullYear();
        });
    });

    return {
        categories,
        icons,
        formatPrice,
        escapeHTML,
        loadProducts,
        productCard
    };
})();
