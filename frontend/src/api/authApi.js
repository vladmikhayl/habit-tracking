const authApi = {
  // Логин
  async login({ username, password }) {
    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ username, password }),
    });

    let data = null;

    try {
      data = await response.json();
    } catch (error) {}

    if (!response.ok) {
      const errorMessage = data?.error || "Ошибка входа";
      throw new Error(errorMessage);
    }

    return data;
  },

  // Регистрация
  async register({ username, password }) {
    const response = await fetch("/api/auth/register", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ username, password }),
    });

    if (!response.ok) {
      let data = null;
      try {
        data = await response.json();
      } catch (error) {}
      let errorMessage = "Ошибка регистрации";
      if (data?.error) {
        errorMessage = data.error;
      } else if (Array.isArray(data?.errors)) {
        errorMessage = data.errors.join("\n");
      }
      throw new Error(errorMessage);
    }
  },
};

export default authApi;
