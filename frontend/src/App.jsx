import ProductList from "./modules/product/ProductList";
import "./App.css";

export default function App() {
  return (
    <main className="app-shell">
      <header className="app-header">
        <p className="eyebrow">COFFEE SHOP POS</p>
        <h1>Quản lý sản phẩm</h1>
        <p className="app-subtitle">Danh mục sản phẩm và trạng thái kinh doanh</p>
      </header>
      <ProductList />
    </main>
  );
}
