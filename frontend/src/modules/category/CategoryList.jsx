import { useCallback, useEffect, useState } from "react";
import {
  activateCategory,
  createCategory,
  deactivateCategory,
  getCategoriesForManagement,
  updateCategory,
} from "./categoryService";

const emptyForm = { name: "", description: "" };

function apiError(error) {
  const data = error?.response?.data;
  if (data?.validationErrors) {
    return Object.values(data.validationErrors).join(" ");
  }
  return data?.message || "Không thể xử lý yêu cầu. Vui lòng thử lại.";
}

export default function CategoryList() {
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [actionId, setActionId] = useState(null);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const loadCategories = useCallback(async () => {
    try {
      setLoading(true);
      setError("");
      setCategories(await getCategoriesForManagement(search));
    } catch (e) {
      setError(apiError(e));
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  function resetForm() {
    setForm(emptyForm);
    setEditingId(null);
    setError("");
  }

  function edit(category) {
    setEditingId(category.id);
    setForm({
      name: category.name ?? "",
      description: category.description ?? "",
    });
    setError("");
    setNotice("");
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    setNotice("");

    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || null,
    };

    if (!payload.name) {
      setError("Vui lòng nhập tên danh mục.");
      return;
    }

    try {
      setSaving(true);
      if (editingId === null) {
        await createCategory(payload);
        setNotice("Đã tạo danh mục.");
      } else {
        await updateCategory(editingId, payload);
        setNotice("Đã cập nhật danh mục.");
      }

      resetForm();
      await loadCategories();
    } catch (e) {
      setError(apiError(e));
    } finally {
      setSaving(false);
    }
  }

  async function toggleStatus(category) {
    setActionId(category.id);
    setError("");
    setNotice("");

    try {
      if (category.active) {
        await deactivateCategory(category.id);
        setNotice(`Đã ngừng sử dụng danh mục "${category.name}".`);
      } else {
        await activateCategory(category.id);
        setNotice(`Đã kích hoạt danh mục "${category.name}".`);
      }
      await loadCategories();
    } catch (e) {
      setError(apiError(e));
    } finally {
      setActionId(null);
    }
  }

  function applySearch(event) {
    event.preventDefault();
    setSearch(searchInput.trim());
  }

  return (
    <section className="management-section">
      <h2>Quản lý danh mục</h2>
      <p className="section-description">
        Danh mục đang hoạt động có thể được gán cho sản phẩm mới. Danh mục
        ngừng hoạt động vẫn được giữ lại để bảo toàn dữ liệu sản phẩm cũ.
      </p>

      {error && <p className="message message-error" role="alert">{error}</p>}
      {notice && <p className="message message-success" role="status">{notice}</p>}

      <form className="product-form" onSubmit={submit}>
        <h3>{editingId === null ? "Thêm danh mục" : `Sửa danh mục #${editingId}`}</h3>

        <label>
          Tên danh mục *
          <input
            name="name"
            value={form.name}
            onChange={(event) => setForm({ ...form, name: event.target.value })}
            maxLength={100}
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

        <div className="form-actions">
          <button type="submit" disabled={saving}>
            {saving ? "Đang lưu..." : editingId === null ? "Tạo danh mục" : "Lưu thay đổi"}
          </button>
          {editingId !== null && (
            <button type="button" className="button-secondary" onClick={resetForm}>
              Hủy sửa
            </button>
          )}
        </div>
      </form>

      <form className="search-form" onSubmit={applySearch}>
        <label>
          Tìm danh mục
          <input
            value={searchInput}
            onChange={(event) => setSearchInput(event.target.value)}
            placeholder="Nhập tên hoặc mô tả..."
          />
        </label>
        <div className="row-actions">
          <button type="submit">Tìm kiếm</button>
          <button
            type="button"
            className="button-secondary"
            onClick={() => {
              setSearchInput("");
              setSearch("");
            }}
          >
            Xóa lọc
          </button>
        </div>
      </form>

      <div className="table-heading">
        <h3>Danh sách danh mục</h3>
        <button
          type="button"
          className="button-secondary"
          onClick={loadCategories}
          disabled={loading}
        >
          Tải lại
        </button>
      </div>

      {loading && categories.length === 0 ? (
        <p role="status">Đang tải danh mục...</p>
      ) : categories.length === 0 ? (
        <p>Không tìm thấy danh mục nào.</p>
      ) : (
        <div className="table-wrapper">
          <table className="product-table">
            <thead>
              <tr>
                <th>Tên danh mục</th>
                <th>Mô tả</th>
                <th>Trạng thái</th>
                <th>Thao tác</th>
              </tr>
            </thead>
            <tbody>
              {categories.map((category) => (
                <tr key={category.id}>
                  <td>{category.name}</td>
                  <td>{category.description || "—"}</td>
                  <td>
                    <span className={category.active ? "status status-active" : "status status-inactive"}>
                      {category.active ? "Đang hoạt động" : "Ngừng hoạt động"}
                    </span>
                  </td>
                  <td>
                    <div className="row-actions">
                      <button type="button" onClick={() => edit(category)}>Sửa</button>
                      <button
                        type="button"
                        className="button-secondary"
                        disabled={actionId === category.id}
                        onClick={() => toggleStatus(category)}
                      >
                        {actionId === category.id
                          ? "Đang xử lý..."
                          : category.active
                            ? "Ngừng hoạt động"
                            : "Kích hoạt"}
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
