package ktorapp.services

import ktorapp.models.PendingRequest
import java.util.concurrent.ConcurrentHashMap

class RequestManager {
    private val pendingRequests = ConcurrentHashMap<String, PendingRequest>()

    fun addRequest(request: PendingRequest) {
        pendingRequests[request.id] = request
    }

    fun removeRequest(id: String): PendingRequest? {
        return pendingRequests.remove(id)
    }

    fun getAllRequests(): Collection<PendingRequest> {
        return pendingRequests.values
    }

    fun getRequestCount(): Int {
        return pendingRequests.size
    }

    fun isEmpty(): Boolean {
        return pendingRequests.isEmpty()
    }
}

