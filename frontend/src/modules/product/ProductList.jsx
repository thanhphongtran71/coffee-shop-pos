import { useCallback, useEffect, useState } from "react";
import {
  activateProduct,
  createProduct,
  deactivateProduct,
  getProductsForManagement,
  updateProduct,
} from "./productService";
import { getCategoriesForManagement } from "../category/categoryService";

const emptyForm = {
  sku: "",
  name: "",
  description: "",
  price: "",
  categoryId: "",
};

function apiError(error) {
  const data = error?.response?.data;
  if (data?.validationErrors) return Object.values(data.validationErrors).join(" ");
  return data?.message || "Không thể xử lý yêu cầu. Vui lòng thử lại.";
}

export default function ProductList() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [categoriesLoading, setCategoriesLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [actionId, setActionId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadProducts = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      setProducts(await getProductsForManagement());
    } catch (e) {
      setError(apiError(e));
    } finally {
      setLoading(false);
    }
  }, []);

  const loadCategories = useCallback(async () => {
    try {
      setCategoriesLoading(true);
      const result = await getCategoriesForManagement();
      setCategories(result);
    } catch (e) {
      setError(apiError(e));
    } finally {
      setCategoriesLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProducts();
    loadCategories();
  }, [loadProducts, loadCategories]);

  function resetForm() {
    setForm(emptyForm);
    setEditingId(null);
    setError("");
  }

  function edit(product) {
    setEditingId(product.id);
    setForm({
      sku: product.sku,
      name: product.name,
      description: product.description || "",
      price: String(product.price),
      categoryId: String(product.categoryId),
    });
    setError("");
    setNotice("");
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    setNotice("");

    const price = Number(form.price);
    const categoryId = Number(form.categoryId);

    if (!Number.isFinite(price) || price <= 0) {
      setError("Giá sản phẩm phải lớn hơn 0.");
      return;
    }
    if (!Number.isInteger(categoryId) || categoryId <= 0) {
      setError("Vui lòng chọn danh mục đang hoạt động.");
      return;
    }

    const payload = {
      sku: form.sku.trim(),
      name: form.name.trim(),
      description: form.description.trim() || null,
      price,
      categoryId,
    };

    try {
      setSaving(true);
      if (editingId === null) {
        await createProduct(payload);
        setNotice("Đã tạo sản phẩm.");
      } else {
        await updateProduct(editingId, payload);
        setNotice("Đã cập nhật sản phẩm.");
      }
      resetForm();
      await loadProducts();
    } catch (e) {
      setError(apiError(e));
    } finally {
      setSaving(false);
    }
  }

  async function toggleStatus(product) {
    setActionId(product.id);
    setError("");
    setNotice("");
    try {
      if (product.active) {
        await deactivateProduct(product.id);
        setNotice(`Đã ngừng bán ${product.name}.`);
      } else {
        await activateProduct(product.id);
        setNotice(`Đã mở bán ${product.name}.`);
      }
      await loadProducts();
    } catch (e) {
      setError(apiError(e));
    } finally {
      setActionId(null);
    }
  }

  // Inactive categories may only remain selected for the product already using them.
  const selectableCategories = categories.filter(
    (category) => category.active || (editingId !== null && String(category.id) === form.categoryId),
  );

  return (
    <section className="management-section">
      <h2>Quản lý sản phẩm</h2>
      {error && <p className="message message-error" role="alert">{error}</p>}
      {notice && <p className="message message-success" role="status">{notice}</p>}

      <form className="product-form" onSubmit={submit}>
        <h3>{editingId === null ? "Thêm sản phẩm" : `Sửa sản phẩm #${editingId}`}</h3>

        <label>
          SKU *
          <input
            name="sku"
            value={form.sku}
            onChange={(event) => setForm({ ...form, sku: event.target.value })}
            maxLength={50}
            required
            autoComplete="off"
          />
        </label>

        <label>
          Tên sản phẩm *
          <input
            name="name"
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            maxLength={150}
            required
          />
        </label>

        <label>
          Mô tả
          <textarea
            name="description"
            value={form.description}
            onChange={(event) => setForm({ ...form, description: event.target.value })}
            maxLength={500}
            rows={3}
          />
        </label>

        <label>
          Giá (VND) *
          <input
            name="price"
            type="number"
            min="0.01"
            step="0.01"
            value={form.price}
            onChange={(event) => setForm({ ...form, price: event.target.value })}
            required
          />
        </label>

        <label>
          Danh mục *
          <select
            name="categoryId"
            value={form.categoryId}
            onChange={(event) => setForm({ ...form, categoryId: event.target.value })}
            required
            disabled={categoriesLoading}
          >
            <option value="">
              {categoriesLoading ? "Đang tải danh mục..." : "— Chọn danh mục —"}
            </option>
            {selectableCategories.map((category) => (
              <option key={category.id} value={String(category.id)}>
                {category.name}{category.active ? "" : " (ngừng hoạt động — chỉ giữ danh mục hiện tại)"}
              </option>
            ))}
          </select>
          {editingId !== null && categories.some(
            (category) => String(category.id) === form.categoryId && !category.active,
          ) && (
            <small className="field-help">
              Danh mục hiện tại đã ngừng hoạt động. Bạn có thể giữ nguyên, nhưng không thể gán danh mục ngừng hoạt động khác.
            </small>
          )}
        </label>

        <div className="form-actions">
          <button type="submit" disabled={saving || categoriesLoading}>
            {saving ? "Đang lưu..." : editingId === null ? "Tạo sản phẩm" : "Lưu thay đổi"}
          </button>
          {editingId !== null && (
            <button type="button" className="button-secondary" onClick={resetForm}>Hủy sửa</button>
          )}
        </div>
      </form>

      <div className="table-heading">
        <h3>Danh sách sản phẩm</h3>
        <button type="button" className="button-secondary" onClick={loadProducts} disabled={loading}>
          Tải lại
        </button>
      </div>

      {loading && products.length === 0 ? (
        <p role="status">Đang tải...</p>
      ) : products.length === 0 ? (
        <p>Chưa có sản phẩm nào.</p>
      ) : (
        <div className="table-wrapper">
          <table className="product-table">
            <thead>
              <tr>
                <th>SKU</th>
                <th>Tên</th>
                <th>Danh mục</th>
                <th>Mô tả</th>
                <th>Giá</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {products.map((product) => (
                <tr key={product.id}>
                  <td>{product.sku}</td>
                  <td>{product.name}</td>
                  <td>{product.categoryName || "—"}</td>
                  <td>{product.description || "—"}</td>
                  <td className="number-cell">{Number(product.price).toLocaleString("vi-VN")} ₫</td>
                  <td>
                    <span className={product.active ? "status status-active" : "status status-inactive"}>
                      {product.active ? "Đang bán" : "Ngừng bán"}
                    </span>
                  </td>
                  <td>
                    <div className="row-actions">
                      <button type="button" onClick={() => edit(product)}>Sửa</button>
                      <button
                        type="button"
                        className="button-secondary"
                        disabled={actionId === product.id}
                        onClick={() => toggleStatus(product)}
                      >
                        {actionId === product.id
                          ? "Đang xử lý..."
                          : product.active ? "Ngừng bán" : "Mở bán"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
