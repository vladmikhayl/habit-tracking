import React from "react";

const FileInput = ({
  selectedFile,
  fileInputRef,
  onFileChange,
  onDeletingSelectedFile,
  inputText,
}) => {
  return (
    <label className="flex items-center justify-between gap-4 cursor-pointer">
      <div className="flex items-center gap-3 text-gray-700">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-6 w-6 text-gray-500"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15.172 7l-6.586 6.586a2 2 0 002.828 2.828L18 9.828m-3-2.828a4 4 0 015.656 0 4 4 0 010 5.656L12 21H3v-9l9-9a4 4 0 015.656 0z"
          />
        </svg>
        <div className="flex items-center gap-2">
          <span className="text-sm sm:text-base">
            {selectedFile ? selectedFile.name : inputText}
          </span>
          {selectedFile && (
            <button
              type="button"
              onClick={(e) => {
                e.stopPropagation();
                e.preventDefault();
                onDeletingSelectedFile();
                if (fileInputRef.current) {
                  fileInputRef.current.value = "";
                }
              }}
              className="text-red-500 hover:text-red-700 text-xl leading-none"
              title="Удалить файл"
            >
              ×
            </button>
          )}
        </div>
      </div>
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        onChange={onFileChange}
        className="hidden"
      />
    </label>
  );
};

export default FileInput;
