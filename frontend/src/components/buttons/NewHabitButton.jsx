import React from "react";
import { useNavigate } from "react-router-dom";

const NewHabitButton = () => {
  const navigate = useNavigate();

  return (
    <button
      onClick={() => navigate("/create-habit")}
      className="bg-green-500 hover:bg-green-600 text-white font-semibold px-6 py-3 rounded-xl transition"
    >
      + Новая привычка
    </button>
  );
};

export default NewHabitButton;
