// ==================== RESIDENT SPECIFIC JAVASCRIPT ====================

console.log('BlueMoon Portal - Resident Module Loaded ✓');

// ==================== FEE MANAGEMENT ====================
function viewFeeDetails(feeId) {
    Alert.loading('Đang tải chi tiết khoản phí...');
    
    setTimeout(() => {
        Alert.close();
        window.location.href = `/resident/fees/${feeId}`;
    }, 500);
}

function payFee(feeId, feeName, amount) {
    Swal.fire({
        title: 'Thanh toán khoản phí',
        html: `
            <div style="text-align: left;">
                <div class="form-group">
                    <label class="form-label">Khoản phí:</label>
                    <input type="text" class="form-control" value="${feeName}" readonly>
                </div>
                <div class="form-group">
                    <label class="form-label">Số tiền:</label>
                    <input type="text" class="form-control" value="${formatCurrency(amount)}" readonly>
                </div>
                <div class="form-group">
                    <label class="form-label">Phương thức thanh toán:</label>
                    <select id="paymentMethod" class="form-control">
                        <option value="bank">Chuyển khoản ngân hàng</option>
                        <option value="momo">Ví MoMo</option>
                        <option value="vnpay">VNPay</option>
                        <option value="office">Thanh toán tại văn phòng</option>
                    </select>
                </div>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: 'Thanh toán',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#4F46E5',
        width: 500
    }).then((result) => {
        if (result.isConfirmed) {
            Alert.loading('Đang xử lý thanh toán...');
            
            setTimeout(() => {
                Alert.success('Thanh toán thành công!', 'Khoản phí đã được thanh toán');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 2000);
        }
    });
}

function viewPaymentHistory() {
    window.location.href = '/resident/payment-history';
}

// ==================== SERVICE MANAGEMENT ====================
function viewServiceDetails(serviceId) {
    Alert.loading('Đang tải thông tin dịch vụ...');
    
    setTimeout(() => {
        Alert.close();
        window.location.href = `/resident/services/${serviceId}`;
    }, 500);
}

function registerService(serviceId, serviceName) {
    Swal.fire({
        title: 'Đăng ký dịch vụ',
        html: `
            <div style="text-align: left;">
                <div class="form-group">
                    <label class="form-label">Dịch vụ:</label>
                    <input type="text" class="form-control" value="${serviceName}" readonly>
                </div>
                <div class="form-group">
                    <label class="form-label">Ngày bắt đầu:</label>
                    <input type="date" id="startDate" class="form-control">
                </div>
                <div class="form-group">
                    <label class="form-label">Ghi chú:</label>
                    <textarea id="serviceNote" class="form-control" rows="3" placeholder="Nhập ghi chú (nếu có)"></textarea>
                </div>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: 'Đăng ký',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#4F46E5',
        width: 500,
        preConfirm: () => {
            const startDate = document.getElementById('startDate').value;
            const note = document.getElementById('serviceNote').value;
            
            if (!startDate) {
                Swal.showValidationMessage('Vui lòng chọn ngày bắt đầu');
                return false;
            }
            
            return { startDate, note };
        }
    }).then((result) => {
        if (result.isConfirmed) {
            Alert.loading('Đang xử lý đăng ký...');
            
            setTimeout(() => {
                Alert.success('Đăng ký thành công!', `Bạn đã đăng ký dịch vụ ${serviceName}`);
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    });
}

function cancelService(serviceId, serviceName) {
    Alert.confirm(
        'Hủy đăng ký dịch vụ',
        `Bạn có chắc muốn hủy đăng ký dịch vụ "${serviceName}"?`,
        () => {
            Alert.loading('Đang xử lý...');
            
            setTimeout(() => {
                Alert.success('Đã hủy!', 'Đăng ký dịch vụ đã được hủy');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== NOTIFICATION MANAGEMENT ====================
function markNotificationAsRead(notificationId) {
    fetch(`/resident/notifications/${notificationId}/read`, {
        method: 'POST'
    })
    .then(response => {
        if (response.ok) {
            const notification = document.getElementById(`notification-${notificationId}`);
            if (notification) {
                notification.classList.remove('unread');
                notification.classList.add('read');
            }
        }
    })
    .catch(error => {
        console.error('Error marking notification as read:', error);
    });
}

function deleteNotification(notificationId) {
    Alert.confirm(
        'Xóa thông báo',
        'Bạn có chắc muốn xóa thông báo này?',
        () => {
            Alert.loading('Đang xóa...');
            
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Thông báo đã được xóa');
                setTimeout(() => {
                    window.location.reload();
                }, 1000);
            }, 500);
        }
    );
}

function markAllNotificationsAsRead() {
    Alert.loading('Đang cập nhật...');
    
    setTimeout(() => {
        Alert.success('Thành công!', 'Tất cả thông báo đã được đánh dấu là đã đọc');
        setTimeout(() => {
            window.location.reload();
        }, 1000);
    }, 500);
}

// ==================== PROFILE MANAGEMENT ====================
function updateProfile() {
    if (!validateForm('profileForm')) {
        return;
    }
    
    Alert.confirm(
        'Xác nhận cập nhật',
        'Bạn có chắc muốn cập nhật thông tin cá nhân?',
        () => {
            Alert.loading('Đang cập nhật...');
            
            setTimeout(() => {
                Alert.success('Cập nhật thành công!', 'Thông tin cá nhân đã được cập nhật');
            }, 1500);
        }
    );
}

function changePassword() {
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    
    if (!currentPassword || !newPassword || !confirmPassword) {
        Alert.error('Lỗi', 'Vui lòng điền đầy đủ thông tin');
        return;
    }
    
    if (newPassword !== confirmPassword) {
        Alert.error('Lỗi', 'Mật khẩu mới và xác nhận mật khẩu không khớp');
        return;
    }
    
    if (newPassword.length < 6) {
        Alert.error('Lỗi', 'Mật khẩu mới phải có ít nhất 6 ký tự');
        return;
    }
    
    Alert.loading('Đang đổi mật khẩu...');
    
    setTimeout(() => {
        Alert.success('Thành công!', 'Mật khẩu đã được thay đổi');
        setTimeout(() => {
            window.location.href = '/logout';
        }, 1500);
    }, 1000);
}

// ==================== REPORT ISSUE ====================
function reportIssue() {
    Swal.fire({
        title: 'Báo cáo sự cố',
        html: `
            <div style="text-align: left;">
                <div class="form-group">
                    <label class="form-label">Loại sự cố:</label>
                    <select id="issueType" class="form-control">
                        <option value="">Chọn loại sự cố</option>
                        <option value="electric">Điện</option>
                        <option value="water">Nước</option>
                        <option value="elevator">Thang máy</option>
                        <option value="parking">Bãi đỗ xe</option>
                        <option value="security">An ninh</option>
                        <option value="other">Khác</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Mô tả chi tiết:</label>
                    <textarea id="issueDescription" class="form-control" rows="4" placeholder="Mô tả chi tiết sự cố..."></textarea>
                </div>
                <div class="form-group">
                    <label class="form-label">Mức độ khẩn cấp:</label>
                    <select id="issuePriority" class="form-control">
                        <option value="low">Thấp</option>
                        <option value="medium">Trung bình</option>
                        <option value="high">Cao</option>
                    </select>
                </div>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: 'Gửi báo cáo',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#4F46E5',
        width: 500,
        preConfirm: () => {
            const type = document.getElementById('issueType').value;
            const description = document.getElementById('issueDescription').value;
            const priority = document.getElementById('issuePriority').value;
            
            if (!type) {
                Swal.showValidationMessage('Vui lòng chọn loại sự cố');
                return false;
            }
            
            if (!description) {
                Swal.showValidationMessage('Vui lòng mô tả chi tiết sự cố');
                return false;
            }
            
            return { type, description, priority };
        }
    }).then((result) => {
        if (result.isConfirmed) {
            Alert.loading('Đang gửi báo cáo...');
            
            setTimeout(() => {
                Alert.success('Đã gửi!', 'Báo cáo sự cố đã được gửi thành công');
            }, 1000);
        }
    });
}

// ==================== RESIDENT INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    console.log('Resident dashboard initialized');
    
    // Mark notification as read when clicked
    const notifications = document.querySelectorAll('.notification-card');
    notifications.forEach(notification => {
        notification.addEventListener('click', function() {
            const notificationId = this.dataset.notificationId;
            if (notificationId && this.classList.contains('unread')) {
                markNotificationAsRead(notificationId);
            }
        });
    });
    
    // Format all currency values
    const currencyElements = document.querySelectorAll('.currency');
    currencyElements.forEach(element => {
        const value = parseFloat(element.textContent);
        if (!isNaN(value)) {
            element.textContent = formatCurrency(value);
        }
    });
});
