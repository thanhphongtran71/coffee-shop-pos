import { useState } from "react";
import ProductList from "./modules/product/ProductList";
import CategoryList from "./modules/category/CategoryList";
import "./App.css";

export default function App() {
  const [activePage, setActivePage] = useState("products");

  return (
    <main className="app-shell">
      <header className="app-header">
        <p className="eyebrow">COFFEE SHOP POS</p>
        <h1>Quản lý cửa hàng</h1>
        <p className="app-subtitle">
          Quản lý sản phẩm, danh mục và trạng thái kinh doanh
        </p>
        <nav className="app-nav" aria-label="Điều hướng quản lý">
          <button
            type="button"
            className={activePage === "products" ? "nav-button nav-button-active" : "nav-button"}
            aria-current={activePage === "products" ? "page" : undefined}
            onClick={() => setActivePage("products")}
          >
            Sản phẩm
          </button>
          <button
            type="button"
            className={activePage === "categories" ? "nav-button nav-button-active" : "nav-button"}
            aria-current={activePage === "categories" ? "page" : undefined}
            onClick={() => setActivePage("categories")}
          >
            Danh mục
          </button>
        </nav>
      </header>

      {activePage === "products" ? <ProductList /> : <CategoryList />}
    </main>
  );
}
