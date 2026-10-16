import api from "../../services/api";

export async function getCategories() {
  return (await api.get("/categories")).data;
}

export async function getCategoriesForManagement(search = "") {
  return (
    await api.get("/categories/management", {
      params: { search: search.trim() },
    })
  ).data;
}

export async function getCategoryById(id) {
  return (await api.get(`/categories/${id}`)).data;
}

export async function createCategory(category) {
  return (await api.post("/categories", category)).data;
}

export async function updateCategory(id, category) {
  return (await api.put(`/categories/${id}`, category)).data;
}

export async function activateCategory(id) {
  return (await api.patch(`/categories/${id}/activate`)).data;
}

export async function deactivateCategory(id) {
  return (await api.patch(`/categories/${id}/deactivate`)).data;
}
