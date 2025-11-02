// ==================== ADMIN SPECIFIC JAVASCRIPT ====================

console.log('BlueMoon Portal - Admin Module Loaded ✓');

// ==================== ADMIN DASHBOARD FUNCTIONS ====================
function refreshAdminStats() {
    Alert.loading('Đang tải dữ liệu thống kê...');
    
    // Simulate API call
    setTimeout(() => {
        Alert.close();
        showNotification('Dữ liệu đã được cập nhật', 'success');
    }, 1000);
}

// ==================== RESIDENT MANAGEMENT ====================
function viewResidentDetails(residentId) {
    Alert.loading('Đang tải thông tin cư dân...');
    
    setTimeout(() => {
        Alert.close();
        // Display resident details
        Swal.fire({
            title: 'Thông Tin Cư Dân',
            html: `
                <div style="text-align: left; padding: 20px;">
                    <p><strong>Mã cư dân:</strong> ${residentId}</p>
                    <p><strong>Họ tên:</strong> Nguyễn Văn A</p>
                    <p><strong>Email:</strong> nguyenvana@example.com</p>
                    <p><strong>Số điện thoại:</strong> 0123456789</p>
                    <p><strong>Căn hộ:</strong> A101</p>
                </div>
            `,
            confirmButtonColor: '#4F46E5',
            width: 600
        });
    }, 500);
}

function deleteResident(residentId, residentName) {
    Alert.confirm(
        'Xác nhận xóa cư dân',
        `Bạn có chắc muốn xóa cư dân "${residentName}"? Hành động này không thể hoàn tác.`,
        () => {
            Alert.loading('Đang xóa...');
            
            // Simulate deletion API call
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Cư dân đã được xóa thành công');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== APARTMENT MANAGEMENT ====================
function viewApartmentDetails(apartmentId) {
    Alert.loading('Đang tải thông tin căn hộ...');
    
    setTimeout(() => {
        Alert.close();
        window.location.href = `/admin/apartments/${apartmentId}`;
    }, 500);
}

function deleteApartment(apartmentId, apartmentCode) {
    Alert.confirm(
        'Xác nhận xóa căn hộ',
        `Bạn có chắc muốn xóa căn hộ "${apartmentCode}"?`,
        () => {
            Alert.loading('Đang xóa...');
            
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Căn hộ đã được xóa thành công');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== SERVICE MANAGEMENT ====================
function deleteService(serviceId, serviceName) {
    Alert.confirm(
        'Xác nhận xóa dịch vụ',
        `Bạn có chắc muốn xóa dịch vụ "${serviceName}"?`,
        () => {
            Alert.loading('Đang xóa...');
            
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Dịch vụ đã được xóa thành công');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== NOTIFICATION MANAGEMENT ====================
function sendNotification() {
    const form = document.getElementById('notificationForm');
    
    if (!validateForm('notificationForm')) {
        return;
    }
    
    Alert.confirm(
        'Xác nhận gửi thông báo',
        'Bạn có chắc muốn gửi thông báo này đến tất cả cư dân?',
        () => {
            Alert.loading('Đang gửi thông báo...');
            
            setTimeout(() => {
                Alert.success('Đã gửi!', 'Thông báo đã được gửi thành công');
                form.reset();
            }, 1500);
        }
    );
}

// ==================== EXPORT FUNCTIONS ====================
function exportToExcel(dataType) {
    Alert.loading('Đang xuất dữ liệu...');
    
    setTimeout(() => {
        Alert.success('Xuất thành công!', `Dữ liệu ${dataType} đã được xuất ra file Excel`);
    }, 1500);
}

function exportToPDF(dataType) {
    Alert.loading('Đang xuất dữ liệu...');
    
    setTimeout(() => {
        Alert.success('Xuất thành công!', `Dữ liệu ${dataType} đã được xuất ra file PDF`);
    }, 1500);
}

// ==================== ADMIN INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    console.log('Admin dashboard initialized');
    
    // Auto-refresh stats every 5 minutes
    setInterval(() => {
        console.log('Auto-refreshing admin stats...');
        // refreshAdminStats(); // Uncomment to enable auto-refresh
    }, 300000);
});
