import React from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";

const MainLayout = ({ children }) => {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    toast.success("Успешный выход из аккаунта");
    navigate("/login");
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-100 to-purple-200 p-4">
      <div className="max-w-4xl mx-auto bg-white rounded-2xl shadow-lg overflow-hidden">
        <nav className="flex justify-between items-center bg-blue-500 text-white px-6 py-4">
          <div className="flex gap-3">
            <Link
              to="/my-habits"
              className="bg-white text-blue-500 font-semibold px-4 py-2 rounded-xl hover:bg-blue-100 transition"
            >
              Мои привычки
            </Link>
            <Link
              to="/subscriptions"
              className="bg-white text-blue-500 font-semibold px-4 py-2 rounded-xl hover:bg-blue-100 transition"
            >
              Мои подписки
            </Link>
          </div>
          <button
            onClick={handleLogout}
            className="bg-white text-blue-500 font-semibold px-4 py-2 rounded-xl hover:bg-blue-100 transition"
          >
            Выйти
          </button>
        </nav>

        <main className="p-6">{children}</main>
      </div>
    </div>
  );
};

export default MainLayout;
