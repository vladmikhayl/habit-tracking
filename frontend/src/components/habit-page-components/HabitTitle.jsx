import React from "react";

const HabitTitle = ({ name, stampText }) => {
  return (
    <div className="flex flex-wrap items-center gap-4 mb-4">
      <h1 className="text-3xl font-bold break-words whitespace-pre-wrap [overflow-wrap:anywhere]">
        {name}
      </h1>
      <span className="text-sm font-semibold px-4 py-2 rounded-full bg-blue-100 text-blue-700">
        {stampText}
      </span>
    </div>
  );
};

export default HabitTitle;
