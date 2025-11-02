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
                <p><strong>Khoản phí:</strong> ${feeName}</p>
                <p><strong>Số tiền:</strong> ${formatCurrency(amount)}</p>
                <label style="display: block; margin-top: 15px; font-weight: 500;">Phương thức thanh toán:</label>
                <select id="paymentMethod" class="form-control">
                    <option value="bank">Chuyển khoản ngân hàng</option>
                    <option value="momo">Ví MoMo</option>
                    <option value="vnpay">VNPay</option>
                    <option value="office">Thanh toán tại văn phòng</option>
                </select>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: 'Thanh toán',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#4F46E5'
    }).then((result) => {
        if (result.isConfirmed) {
            Alert.loading('Đang xử lý thanh toán...');
            setTimeout(() => {
                Alert.success('Thanh toán thành công!', 'Khoản phí đã được thanh toán');
                setTimeout(() => window.location.reload(), 1500);
            }, 2000);
        }
    });
}

// ==================== SERVICE MANAGEMENT ====================
function registerService(serviceId, serviceName) {
    Swal.fire({
        title: 'Đăng ký dịch vụ',
        html: `
            <div style="text-align: left;">
                <p><strong>Dịch vụ:</strong> ${serviceName}</p>
                <label style="display: block; margin-top: 15px; font-weight: 500;">Ngày bắt đầu:</label>
                <input type="date" id="startDate" class="form-control">
                <label style="display: block; margin-top: 15px; font-weight: 500;">Ghi chú:</label>
                <textarea id="serviceNote" class="form-control" rows="3" placeholder="Nhập ghi chú (nếu có)"></textarea>
            </div>
        `,
        showCancelButton: true,
        confirmButtonText: 'Đăng ký',
        cancelButtonText: 'Hủy',
        confirmButtonColor: '#4F46E5'
    }).then((result) => {
        if (result.isConfirmed) {
            Alert.loading('Đang xử lý đăng ký...');
            setTimeout(() => {
                Alert.success('Đăng ký thành công!', `Bạn đã đăng ký dịch vụ ${serviceName}`);
                setTimeout(() => window.location.reload(), 1500);
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
                setTimeout(() => window.location.reload(), 1500);
            }, 1000);
        }
    );
}

// ==================== PROFILE ====================
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
        setTimeout(() => window.location.href = '/logout', 1500);
    }, 1000);
}
