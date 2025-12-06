// ==================== SIDEBAR TOGGLE ====================
function toggleSidebar() {
    const sidebar = document.querySelector('.sidebar');
    sidebar.classList.toggle('active');
}

// ==================== SWEETALERT HELPERS ====================
const Alert = {
    success: (title, text) => {
        Swal.fire({
            icon: 'success',
            title: title,
            text: text,
            confirmButtonColor: '#4F46E5'
        });
    },

    error: (title, text) => {
        Swal.fire({
            icon: 'error',
            title: title,
            text: text,
            confirmButtonColor: '#EF4444'
        });
    },

    confirm: (title, text, callback) => {
        Swal.fire({
            title: title,
            text: text,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#4F46E5',
            cancelButtonColor: '#6B7280',
            confirmButtonText: 'Xác nhận',
            cancelButtonText: 'Hủy'
        }).then((result) => {
            if (result.isConfirmed) {
                callback();
            }
        });
    },

    loading: (title) => {
        Swal.fire({
            title: title,
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });
    },

    close: () => {
        Swal.close();
    }
};

// ==================== DELETE CONFIRMATION ====================
function confirmDelete(id, name, deleteUrl) {
    Alert.confirm(
        'Xác nhận xóa',
        `Bạn có chắc muốn xóa "${name}"? Hành động này không thể hoàn tác.`,
        () => {
            Alert.loading('Đang xóa...');

            // Simulate deletion (trong demo)
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Dữ liệu đã được xóa thành công');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== FORM VALIDATION ====================
function validateForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return false;

    let isValid = true;
    const requiredFields = form.querySelectorAll('[required]');

    requiredFields.forEach(field => {
        if (!field.value.trim()) {
            isValid = false;
            field.classList.add('error');

            if (!field.nextElementSibling || !field.nextElementSibling.classList.contains('form-error')) {
                const errorMsg = document.createElement('div');
                errorMsg.className = 'form-error';
                errorMsg.textContent = 'Trường này là bắt buộc';
                field.parentNode.insertBefore(errorMsg, field.nextSibling);
            }
        } else {
            field.classList.remove('error');
            if (field.nextElementSibling && field.nextElementSibling.classList.contains('form-error')) {
                field.nextElementSibling.remove();
            }
        }
    });

    return isValid;
}

// Remove error on input
document.addEventListener('DOMContentLoaded', () => {
    const inputs = document.querySelectorAll('.form-control');
    inputs.forEach(input => {
        input.addEventListener('input', function () {
            this.classList.remove('error');
            if (this.nextElementSibling && this.nextElementSibling.classList.contains('form-error')) {
                this.nextElementSibling.remove();
            }
        });
    });
});

// ==================== TABLE SEARCH ====================
function searchTable(inputId, tableId) {
    const input = document.getElementById(inputId);
    const table = document.getElementById(tableId);
    const filter = input.value.toLowerCase();
    const rows = table.getElementsByTagName('tr');

    for (let i = 1; i < rows.length; i++) {
        const cells = rows[i].getElementsByTagName('td');
        let found = false;

        for (let j = 0; j < cells.length; j++) {
            const cell = cells[j];
            if (cell) {
                const textValue = cell.textContent || cell.innerText;
                if (textValue.toLowerCase().indexOf(filter) > -1) {
                    found = true;
                    break;
                }
            }
        }

        rows[i].style.display = found ? '' : 'none';
    }
}

// ==================== VIEW RESIDENT ====================
function viewResident(cccd) {
    Alert.loading('Đang tải thông tin...');
    setTimeout(() => {
        Alert.close();
        Swal.fire({
            title: 'Thông Tin Cư Dân',
            html: `
                <div style="text-align: left; padding: 20px;">
                    <p><strong>CCCD:</strong> ${cccd}</p>
                    <p><strong>Họ tên:</strong> Nguyễn Văn Thanh</p>
                    <p><strong>Email:</strong> thanh.nv@gmail.com</p>
                    <p><strong>Số điện thoại:</strong> 0912345678</p>
                </div>
            `,
            confirmButtonColor: '#4F46E5'
        });
    }, 500);
}

// ==================== DATE FORMATTING ====================
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
    });
}

function formatDateTime(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('vi-VN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// ==================== CURRENCY FORMATTING ====================
function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

// ==================== NOTIFICATION ====================
function showNotification(message, type = 'info') {
    const colors = {
        success: '#10B981',
        error: '#EF4444',
        warning: '#F59E0B',
        info: '#3B82F6'
    };

    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${colors[type]};
        color: white;
        padding: 16px 24px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        z-index: 10000;
        animation: slideIn 0.3s ease;
    `;
    notification.textContent = message;

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => notification.remove(), 300);
    }, 3000);
}

// Add animation CSS
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOut {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);

console.log('BlueMoon Portal - Main JS Loaded ✓');

// ==================== NOTIFICATION SYSTEM FOR RESIDENTS ====================
class ResidentNotificationSystem {
    constructor() {
        this.notifications = [];
        this.unreadCount = 0;
        this.isOpen = false;
        this.refreshInterval = null;
        
        // Chỉ khởi tạo nếu user là cư dân
        if (this.isResidentPage()) {
            this.init();
        }
    }

    isResidentPage() {
        // Kiểm tra xem có phải trang của cư dân không
        return window.location.pathname.includes('/resident/');
    }

    init() {
        this.injectNotificationUI();
        this.attachEventListeners();
        this.loadNotifications();
        this.startAutoRefresh();
    }

    injectNotificationUI() {
        const headerRight = document.querySelector('.header-right');
        if (!headerRight) return;

        const notificationHTML = `
            <div class="notification-wrapper" style="position: relative; margin-right: 20px;">
                <button class="notification-bell" id="notificationBell" title="Thông báo">
                    <i class="fas fa-bell"></i>
                    <span class="notification-badge" id="notificationBadge" style="display: none;">0</span>
                </button>
                <div class="notification-dropdown" id="notificationDropdown">
                    <div class="notification-header">
                        <h3><i class="fas fa-bell"></i> Thông báo</h3>
                        <button class="mark-all-read-btn" id="markAllReadBtn" title="Đánh dấu tất cả đã đọc">
                            <i class="fas fa-check-double"></i>
                        </button>
                    </div>
                    <div class="notification-list" id="notificationList">
                        <div class="notification-loading">
                            <i class="fas fa-spinner fa-spin"></i>
                            <p>Đang tải thông báo...</p>
                        </div>
                    </div>
                    <div class="notification-footer">
                        <a href="/resident/notifications" class="view-all-link">
                            Xem tất cả thông báo <i class="fas fa-arrow-right"></i>
                        </a>
                    </div>
                </div>
            </div>
        `;

        // Insert before user-menu
        const userMenu = headerRight.querySelector('.user-menu');
        if (userMenu) {
            userMenu.insertAdjacentHTML('beforebegin', notificationHTML);
        } else {
            headerRight.insertAdjacentHTML('afterbegin', notificationHTML);
        }
    }

    attachEventListeners() {
        const bell = document.getElementById('notificationBell');
        const dropdown = document.getElementById('notificationDropdown');
        const markAllBtn = document.getElementById('markAllReadBtn');

        if (bell) {
            bell.addEventListener('click', (e) => {
                e.stopPropagation();
                this.toggleDropdown();
            });
        }

        if (markAllBtn) {
            markAllBtn.addEventListener('click', (e) => {
                e.stopPropagation();
                this.markAllAsRead();
            });
        }

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (dropdown && !dropdown.contains(e.target) && !bell.contains(e.target)) {
                this.closeDropdown();
            }
        });
    }

    toggleDropdown() {
        const dropdown = document.getElementById('notificationDropdown');
        if (!dropdown) return;

        this.isOpen = !this.isOpen;
        dropdown.classList.toggle('show', this.isOpen);

        if (this.isOpen) {
            this.loadNotifications();
        }
    }

    closeDropdown() {
        const dropdown = document.getElementById('notificationDropdown');
        if (dropdown) {
            dropdown.classList.remove('show');
            this.isOpen = false;
        }
    }

    async loadNotifications() {
        try {
            const response = await fetch('/api/resident/notifications');
            if (!response.ok) throw new Error('Failed to load notifications');

            const data = await response.json();
            this.notifications = data.slice(0, 10); // Chỉ lấy 10 thông báo mới nhất
            this.renderNotifications();
            this.updateBadge();
        } catch (error) {
            console.error('Error loading notifications:', error);
            this.showError();
        }
    }

    // Lấy danh sách ID thông báo đã đọc từ localStorage
    getReadNotifications() {
        const read = localStorage.getItem('readNotifications');
        return read ? JSON.parse(read) : [];
    }

    // Lưu ID thông báo đã đọc vào localStorage
    saveReadNotification(notificationId) {
        const readList = this.getReadNotifications();
        if (!readList.includes(notificationId)) {
            readList.push(notificationId);
            localStorage.setItem('readNotifications', JSON.stringify(readList));
        }
    }

    // Kiểm tra xem thông báo đã đọc chưa
    isNotificationRead(notificationId) {
        return this.getReadNotifications().includes(notificationId);
    }

    renderNotifications() {
        const listContainer = document.getElementById('notificationList');
        if (!listContainer) return;

        if (this.notifications.length === 0) {
            listContainer.innerHTML = `
                <div class="no-notifications">
                    <i class="fas fa-bell-slash"></i>
                    <p>Không có thông báo mới</p>
                </div>
            `;
            return;
        }

        listContainer.innerHTML = this.notifications.map(notif => {
            const isRead = this.isNotificationRead(notif.maThongBao);
            const iconClass = this.getNotificationIcon(notif.loaiThongBao);
            const timeAgo = this.formatTimeAgo(notif.thoiGianGui);

            return `
                <div class="notification-item ${!isRead ? 'unread' : ''}" 
                     data-id="${notif.maThongBao}"
                     onclick="residentNotificationSystem.viewNotification(${notif.maThongBao})">
                    <div class="notification-icon ${notif.loaiThongBao.toLowerCase()}">
                        <i class="${iconClass}"></i>
                    </div>
                    <div class="notification-content">
                        <div class="notification-title">${this.escapeHtml(notif.tieuDe)}</div>
                        <div class="notification-body">${this.truncate(this.escapeHtml(notif.noiDung), 80)}</div>
                        <div class="notification-time">
                            <i class="fas fa-clock"></i> ${timeAgo}
                        </div>
                    </div>
                    ${isRead ? '<div class="read-indicator"><i class="fas fa-check-circle"></i></div>' : '<div class="unread-dot"></div>'}
                </div>
            `;
        }).join('');
    }

    async viewNotification(id) {
        try {
            // Đánh dấu đã đọc trong localStorage
            this.saveReadNotification(id);
            this.updateBadge();

            // Lấy chi tiết thông báo
            const response = await fetch(`/api/resident/notifications/${id}`);
            if (!response.ok) throw new Error('Failed to load notification details');

            const data = await response.json();

            // Hiển thị modal
            const iconClass = this.getNotificationIcon(data.loaiThongBao);
            const typeColors = {
                'khan_cap': '#EF4444',
                'quan_trong': '#F59E0B',
                'binh_thuong': '#3B82F6'
            };

            Swal.fire({
                title: `<i class="${iconClass}" style="color: ${typeColors[data.loaiThongBao] || '#3B82F6'}"></i> ${data.tieuDe}`,
                html: `
                    <div class="notification-detail-modal">
                        <div class="notification-meta">
                            <span><i class="fas fa-user"></i> ${this.escapeHtml(data.nguoiGui || 'Ban Quản Trị')}</span>
                            <span><i class="fas fa-clock"></i> ${this.formatDateTime(data.thoiGianGui)}</span>
                        </div>
                        <div class="notification-content-full">
                            ${this.escapeHtml(data.noiDung).replace(/\n/g, '<br>')}
                        </div>
                        ${data.phanHoi && data.phanHoi.length > 0 ? `
                            <div class="notification-replies">
                                <h4><i class="fas fa-comments"></i> Phản hồi (${data.phanHoi.length})</h4>
                                ${data.phanHoi.map(reply => `
                                    <div class="reply-item">
                                        <div class="reply-header">
                                            <strong>${this.escapeHtml(reply.nguoiGui || 'Người dùng')}</strong>
                                            <span>${this.formatDateTime(reply.thoiGianGui)}</span>
                                        </div>
                                        <div class="reply-content">${this.escapeHtml(reply.noiDung)}</div>
                                    </div>
                                `).join('')}
                            </div>
                        ` : ''}
                    </div>
                `,
                width: '600px',
                confirmButtonText: 'Đóng',
                confirmButtonColor: '#4F46E5',
                customClass: {
                    popup: 'notification-modal'
                }
            });

            // Reload notifications để cập nhật trạng thái
            this.loadNotifications();
        } catch (error) {
            console.error('Error viewing notification:', error);
            if (typeof Swal !== 'undefined') {
                Swal.fire('Lỗi', 'Không thể tải chi tiết thông báo', 'error');
            }
        }
    }

    markAllAsRead() {
        try {
            // Đánh dấu tất cả thông báo hiện tại là đã đọc trong localStorage
            this.notifications.forEach(notif => {
                this.saveReadNotification(notif.maThongBao);
            });
            
            this.renderNotifications();
            this.updateBadge();
            
            if (typeof showNotification !== 'undefined') {
                showNotification('Đã đánh dấu tất cả thông báo là đã đọc', 'success');
            }
        } catch (error) {
            console.error('Error marking all as read:', error);
        }
    }

    updateBadge() {
        const badge = document.getElementById('notificationBadge');
        if (!badge) return;

        const unreadCount = this.notifications.filter(n => !this.isNotificationRead(n.maThongBao)).length;
        this.unreadCount = unreadCount;

        if (unreadCount > 0) {
            badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
            badge.style.display = 'flex';
        } else {
            badge.style.display = 'none';
        }
    }

    startAutoRefresh() {
        // Refresh mỗi 30 giây
        this.refreshInterval = setInterval(() => {
            if (!this.isOpen) {
                this.loadNotifications();
            }
        }, 30000);
    }

    showError() {
        const listContainer = document.getElementById('notificationList');
        if (listContainer) {
            listContainer.innerHTML = `
                <div class="notification-error">
                    <i class="fas fa-exclamation-triangle"></i>
                    <p>Không thể tải thông báo</p>
                    <button onclick="residentNotificationSystem.loadNotifications()" class="retry-btn">
                        <i class="fas fa-redo"></i> Thử lại
                    </button>
                </div>
            `;
        }
    }

    getNotificationIcon(type) {
        const icons = {
            'khan_cap': 'fas fa-exclamation-circle',
            'quan_trong': 'fas fa-exclamation-triangle',
            'binh_thuong': 'fas fa-info-circle'
        };
        return icons[type] || 'fas fa-bell';
    }

    formatTimeAgo(dateTimeString) {
        const now = new Date();
        const past = new Date(dateTimeString);
        const seconds = Math.floor((now - past) / 1000);

        let interval = seconds / 31536000;
        if (interval > 1) return Math.floor(interval) + " năm trước";
        interval = seconds / 2592000;
        if (interval > 1) return Math.floor(interval) + " tháng trước";
        interval = seconds / 86400;
        if (interval > 1) return Math.floor(interval) + " ngày trước";
        interval = seconds / 3600;
        if (interval > 1) return Math.floor(interval) + " giờ trước";
        interval = seconds / 60;
        if (interval > 1) return Math.floor(interval) + " phút trước";
        return "vừa xong";
    }

    formatDateTime(dateTimeString) {
        const date = new Date(dateTimeString);
        return date.toLocaleString('vi-VN', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    truncate(str, length) {
        return str.length > length ? str.substring(0, length) + '...' : str;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    getCsrfToken() {
        const meta = document.querySelector('meta[name="_csrf"]');
        return meta ? meta.content : '';
    }
}

// Khởi tạo notification system khi DOM ready
let residentNotificationSystem;
document.addEventListener('DOMContentLoaded', () => {
    residentNotificationSystem = new ResidentNotificationSystem();
});

