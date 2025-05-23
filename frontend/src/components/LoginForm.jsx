import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { toast } from "react-toastify";

import authApi from "../api/authApi";
import SubmitButton from "../components/buttons/SubmitButton";

const LoginForm = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  // При нажатии на кнопку для входа в аккаунт
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    try {
      const data = await authApi.login({ username, password });
      console.log("Токен:", data.token);
      localStorage.setItem("token", data.token); // Устанавливаем токен в localStorage
      toast.success("Успешный вход в аккаунт");
      navigate("/my-habits");
    } catch (err) {
      console.error(err);
      setError(err.message);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-8">
      <h2 className="text-3xl font-bold text-center text-gray-800">
        Вход в систему
      </h2>

      {error && (
        <div className="p-3 bg-red-100 text-red-700 rounded-xl text-center">
          {error}
        </div>
      )}

      <div>
        <label
          htmlFor="login"
          className="block text-base font-medium text-gray-700"
        >
          Логин
        </label>
        <input
          type="text"
          id="login"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          className="mt-1 w-full p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
        />
      </div>

      <div>
        <label
          htmlFor="password"
          className="block text-base font-medium text-gray-700"
        >
          Пароль
        </label>
        <input
          type="password"
          id="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          className="mt-1 w-full p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
        />
      </div>

      <SubmitButton>Войти</SubmitButton>

      <p className="text-center text-base text-gray-600">
        Нет аккаунта?{" "}
        <Link to="/register" className="text-blue-500 hover:underline">
          Зарегистрироваться
        </Link>
      </p>
    </form>
  );
};

export default LoginForm;
