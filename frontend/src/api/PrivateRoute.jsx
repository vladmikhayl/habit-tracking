import React from "react";
import { useEffect, useState } from "react";
import { Navigate } from "react-router-dom";

// Этот route проверяет хранимый в localStorage токен
// Если он верный, то контент отображается, а если нет - юзер перенаправляется на /login
const PrivateRoute = ({ children }) => {
  const [isValid, setIsValid] = useState(null);

  useEffect(() => {
    const checkToken = async () => {
      const token = localStorage.getItem("token");
      if (!token) {
        setIsValid(false);
        return;
      }

      try {
        const response = await fetch(
          "/api/habits/all-user-habits/at-day/2025-01-01",
          {
            method: "GET",
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );

        setIsValid(response.ok);
        if (!response.ok) localStorage.removeItem("token");
      } catch (err) {
        setIsValid(false);
        localStorage.removeItem("token");
      }
    };

    checkToken();
  }, []);

  if (isValid === null) {
    return <div className="text-center mt-8">Проверка авторизации...</div>;
  }

  return isValid ? children : <Navigate to="/login" replace />;
};

export default PrivateRoute;
