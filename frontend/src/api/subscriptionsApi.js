const subscriptionsApi = {
  // Список принятых подписчиков на привычку
  getHabitAcceptedSubscriptions: async (habitId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/subscriptions/${habitId}/get-habit-accepted-subscriptions`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось получить список подписчиков");
    }

    return await response.json();
  },

  // Список необработанных заявок на подписку на привычку
  getHabitUnprocessedRequests: async (habitId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/subscriptions/${habitId}/get-habit-unprocessed-requests`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось получить список подписчиков");
    }

    return await response.json();
  },

  // Список принятых подписок юзера
  getUserAcceptedSubscriptions: async () => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/subscriptions/get-user-accepted-subscriptions`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось получить список подписок");
    }

    return await response.json();
  },

  // Список необработанных заявок на подписку юзера
  getUserUnprocessedRequests: async () => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/subscriptions/get-user-unprocessed-requests`,
      {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось получить список подписок");
    }

    return await response.json();
  },

  // Принять заявку
  acceptSubscriptionRequest: async (subscriptionId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/subscriptions/${subscriptionId}/accept`,
      {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      throw new Error("Не удалось принять заявку");
    }
  },

  // Отклонить заявку
  denySubscriptionRequest: async (subscriptionId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/subscriptions/${subscriptionId}/deny`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error("Не удалось отклонить заявку");
    }
  },

  // Отправить заявку
  sendSubscriptionRequest: async (habitId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(
      `/api/subscriptions/${habitId}/send-subscription-request`,
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
      }
    );

    if (!response.ok) {
      let data = null;
      try {
        data = await response.json();
      } catch (error) {}
      let errorMessage = "Не удалось отправить заявку";
      if (data?.error) {
        if (data.error.startsWith("For input string")) {
          errorMessage = "Некорректный ID привычки";
        } else {
          errorMessage = data.error;
        }
      }
      throw new Error(errorMessage);
    }
  },

  // Отписаться
  unsubscribe: async (habitId) => {
    const token = localStorage.getItem("token");

    const response = await fetch(`/api/subscriptions/${habitId}/unsubscribe`, {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
    });

    if (!response.ok) {
      throw new Error("Не удалось отписаться");
    }
  },
};

export default subscriptionsApi;
