import React from "react";

const LearnMoreAboutHabitButton = ({ onClick }) => {
  return (
    <button
      onClick={onClick}
      className="w-full border border-gray-400 text-gray-700 hover:bg-gray-100 font-semibold px-5 py-2 rounded-xl transition"
    >
      Подробнее о привычке
    </button>
  );
};

export default LearnMoreAboutHabitButton;
