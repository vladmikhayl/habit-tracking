import React, { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import authApi from "../api/authApi";
import { toast } from "react-toastify";

const RegisterForm = () => {
  const navigate = useNavigate();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (password !== confirmPassword) {
      setError("Пароли не совпадают");
      return;
    }

    try {
      const data = await authApi.register({ username, password });
      console.log("Зарегистрирован:", username, ",", password);
      toast.success("Аккаунт успешно зарегистрирован");
      navigate("/login");
    } catch (err) {
      console.error(err);
      setError(err.message);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-8">
      <h2 className="text-3xl font-bold text-center text-gray-800">
        Регистрация
      </h2>

      {error && (
        <div className="p-3 bg-red-100 text-red-700 rounded-xl text-center whitespace-pre-line">
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

      <div>
        <label
          htmlFor="confirmPassword"
          className="block text-base font-medium text-gray-700"
        >
          Подтверждение пароля
        </label>
        <input
          type="password"
          id="confirmPassword"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
          className="mt-1 w-full p-3 border rounded-xl focus:ring-2 focus:ring-blue-400 focus:outline-none"
        />
      </div>

      <button
        type="submit"
        className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold text-lg py-2 px-4 rounded-xl transition"
      >
        Зарегистрироваться
      </button>

      <p className="text-center text-base text-gray-600">
        Уже есть аккаунт?{" "}
        <Link to="/login" className="text-blue-500 hover:underline">
          Войти
        </Link>
      </p>
    </form>
  );
};

export default RegisterForm;
