import api from "../../services/api";

export async function getProducts() {
  return (await api.get("/products")).data;
}
export async function getProductsForManagement() {
  return (await api.get("/products/management")).data;
}
export async function getProductById(id) {
  return (await api.get(`/products/${id}`)).data;
}
export async function createProduct(product) {
  return (await api.post("/products", product)).data;
}
export async function updateProduct(id, product) {
  return (await api.put(`/products/${id}`, product)).data;
}
export async function activateProduct(id) {
  return (await api.patch(`/products/${id}/activate`)).data;
}
export async function deactivateProduct(id) {
  return (await api.patch(`/products/${id}/deactivate`)).data;
}
