
"use strict";

document.addEventListener("DOMContentLoaded", async () => {
    const grid = document.getElementById("products-grid");
    if (!grid) return;

    const search = document.getElementById("search");
    const category = document.getElementById("category");
    const condition = document.getElementById("condition");
    const price = document.getElementById("price");
    const sort = document.getElementById("sort");

    const count = document.getElementById("result-count");
    const empty = document.getElementById("empty-state");

    const dialog = document.getElementById("product-dialog");
    const dialogContent = document.getElementById("dialog-content");

    let allProducts = [];

    function normalize(text) {
        return String(text)
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .replace(/đ/g, "d")
            .replace(/Đ/g, "D")
            .toLowerCase();
    }

    function filterProducts() {
        const keyword = normalize(search.value.trim());

        let result = allProducts.filter(product => {
            const matchesSearch = normalize(
                product.name + " " + product.description
            ).includes(keyword);

            const matchesCategory =
                category.value === "all" ||
                product.category === category.value;

            const matchesCondition =
                condition.value === "all" ||
                product.condition === condition.value;

            let matchesPrice = true;

            if (price.value === "under1") {
                matchesPrice = product.price < 1000000;
            } else if (price.value === "1to5") {
                matchesPrice =
                    product.price >= 1000000 &&
                    product.price <= 5000000;
            } else if (price.value === "over5") {
                matchesPrice = product.price > 5000000;
            }

            return matchesSearch &&
                   matchesCategory &&
                   matchesCondition &&
                   matchesPrice;
        });

        if (sort.value === "asc") {
            result.sort((a, b) => a.price - b.price);
        } else if (sort.value === "desc") {
            result.sort((a, b) => b.price - a.price);
        } else if (sort.value === "name") {
            result.sort((a, b) =>
                a.name.localeCompare(b.name, "vi")
            );
        }

        return result;
    }

    function renderProducts() {
        const result = filterProducts();

        count.textContent = `Tìm thấy ${result.length} sản phẩm`;

        grid.innerHTML = result
            .map(TIVAREK.productCard)
            .join("");

        empty.hidden = result.length !== 0;
    }

    function resetFilters() {
        document.getElementById("filter-form").reset();
        renderProducts();
    }

    function showProduct(id) {
        const product = allProducts.find(p => p.id === id);

        if (!product) return;

        const e = TIVAREK.escapeHTML;

        const specs = product.specs
            .map(spec => `<li>${e(spec)}</li>`)
            .join("");

        const icon = TIVAREK.icons[product.category] || "📦";
        const categoryName =
            TIVAREK.categories[product.category] || "Khác";

        dialogContent.innerHTML = `
            <div class="dialog-art" aria-hidden="true">${icon}</div>

            <p class="product-category">${e(categoryName)}</p>

            <h2 id="dialog-title">${e(product.name)}</h2>

            <p class="dialog-price">
                ${TIVAREK.formatPrice(product.price)}
            </p>

            <p>${e(product.description)}</p>

            <ul class="dialog-specs">${specs}</ul>

            <p><strong>Tình trạng:</strong>
                ${product.condition === "new" ? "Mới" : "Đã qua sử dụng"}
            </p>

            <p><strong>Bảo hành mẫu:</strong>
                ${Number(product.warrantyMonths)} tháng
            </p>

            <p><strong>Tồn kho mẫu:</strong>
                ${Number(product.stock)}
            </p>

            <div class="dialog-note">
                Đây là thông tin minh họa.
                Chưa thể đặt mua sản phẩm trực tuyến.
            </div>
        `;

        dialog.showModal();
    }

    // Khi nhấn nút xem chi tiết trong danh sách
    grid.addEventListener("click", event => {
        const button = event.target.closest("[data-view]");

        if (!button) return;

        event.preventDefault();
        showProduct(button.dataset.view);
    });

    document.getElementById("close-dialog")
        .addEventListener("click", () => dialog.close());

    dialog.addEventListener("click", event => {
        if (event.target === dialog) dialog.close();
    });

    search.addEventListener("input", renderProducts);

    [category, condition, price, sort].forEach(element => {
        element.addEventListener("change", renderProducts);
    });

    document.getElementById("reset-filters")
        .addEventListener("click", resetFilters);

    document.getElementById("empty-reset")
        .addEventListener("click", resetFilters);

    document.getElementById("filter-form")
        .addEventListener("submit", event => {
            event.preventDefault();
            renderProducts();
        });

    try {
        allProducts = await TIVAREK.loadProducts();

        // Đọc URL, ví dụ: products.html?category=laptop
        const params = new URLSearchParams(window.location.search);

        const requestedCategory = params.get("category");

        if (requestedCategory &&
            [...category.options].some(
                option => option.value === requestedCategory
            )) {
            category.value = requestedCategory;
        }

        renderProducts();

        // Mở chi tiết khi vào từ trang chủ
        const requestedItem = params.get("item");

        if (requestedItem) {
            showProduct(requestedItem);
        }

    } catch (error) {
        console.error(error);

        grid.textContent =
            "Không tải được sản phẩm. Kiểm tra DATA/products.json và local server.";

        count.textContent = "Lỗi tải dữ liệu";
    }
});
