import React from "react";

const SubmitButton = ({ children }) => {
  return (
    <button
      type="submit"
      className="w-full bg-blue-500 hover:bg-blue-600 text-white font-semibold text-lg py-3 px-4 rounded-xl transition"
    >
      {children}
    </button>
  );
};

export default SubmitButton;
