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

// ... (Các hàm khác như toggleSidebar, closeModal, loadReplies)

// ------------------- USER DROPDOWN LOGIC -------------------
// Toggle User Dropdown
function toggleUserDropdown() {
    // Tìm phần tử dropdown bằng ID
    const dropdown = document.getElementById('userDropdown'); 
    if (dropdown) {
        // Thêm/bỏ class 'show'
        dropdown.classList.toggle('show'); 
    }
}

// Close dropdown when clicking outside
document.addEventListener('click', function(e) {
    const menu = document.getElementById('userMenu');
    // Đảm bảo menu tồn tại và click không nằm trong menu
    if (menu && !menu.contains(e.target)) {
        const dropdown = document.getElementById('userDropdown');
        if (dropdown) {
            dropdown.classList.remove('show');
        }
    }
});
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

