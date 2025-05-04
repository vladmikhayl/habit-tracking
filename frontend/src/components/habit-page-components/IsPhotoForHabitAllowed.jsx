import React from "react";
import { CameraIcon, NoSymbolIcon } from "@heroicons/react/24/outline";

const IsPhotoForHabitAllowed = ({ isPhotoAllowed }) => {
  return (
    <div
      className={`mb-4 text-base flex items-center gap-2 font-semibold ${
        isPhotoAllowed ? "text-blue-700" : "text-gray-700"
      }`}
    >
      {isPhotoAllowed ? (
        <>
          <CameraIcon className="h-5 w-5 text-blue-600" />
          Привычка с фотоотчётами
        </>
      ) : (
        <>
          <NoSymbolIcon className="h-5 w-5 text-gray-500" />
          Привычка без фотоотчётов
        </>
      )}
    </div>
  );
};

export default IsPhotoForHabitAllowed;
