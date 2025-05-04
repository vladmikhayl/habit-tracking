import React from "react";
import { useNavigate } from "react-router-dom";

const EditHabitButton = ({ habitId }) => {
  const navigate = useNavigate();

  return (
    <button
      onClick={() => navigate(`/habits/${habitId}/edit`)}
      className="px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-xl text-sm"
    >
      Редактировать привычку
    </button>
  );
};

export default EditHabitButton;
