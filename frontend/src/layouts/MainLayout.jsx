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
        <nav className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3 bg-blue-500 text-white px-6 py-4">
          <div className="flex gap-3">
            <Link
              to="/my-habits"
              className="w-full sm:w-auto text-center bg-white text-blue-500 font-semibold px-4 py-2 rounded-xl hover:bg-blue-100 transition flex items-center justify-center"
            >
              Мои привычки
            </Link>
            <Link
              to="/my-subscriptions"
              className="w-full sm:w-auto text-center bg-white text-blue-500 font-semibold px-4 py-2 rounded-xl hover:bg-blue-100 transition flex items-center justify-center"
            >
              Мои подписки
            </Link>
          </div>
          <button
            onClick={handleLogout}
            className="w-full sm:w-auto text-center bg-white text-blue-500 font-semibold px-4 py-2 rounded-xl hover:bg-blue-100 transition flex items-center justify-center"
          >
            Выйти из аккаунта
          </button>
        </nav>

        <main className="p-6">{children}</main>
      </div>
    </div>
  );
};

export default MainLayout;
