import React from "react";

const HabitDesctiption = ({ description }) => {
  return (
    <>
      {description && description.trim() && (
        <>
          <hr className="my-2 border-gray-300" />
          <div>
            <span className="text-gray-500">Описание:</span>
            <p className="mt-1 text-base text-gray-800 break-words whitespace-pre-wrap [overflow-wrap:anywhere]">
              {description}
            </p>
          </div>
        </>
      )}
    </>
  );
};

export default HabitDesctiption;
