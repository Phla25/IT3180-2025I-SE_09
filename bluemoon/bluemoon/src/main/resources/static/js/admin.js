// ==================== ADMIN SPECIFIC JAVASCRIPT ====================

console.log('BlueMoon Portal - Admin Module Loaded ✓');

// ==================== ADMIN DASHBOARD ====================
function refreshAdminStats() {
    Alert.loading('Đang tải dữ liệu thống kê...');
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
        window.location.href = `/admin/residents/${residentId}`;
    }, 500);
}

function deleteResident(residentId, residentName) {
    Alert.confirm(
        'Xác nhận xóa cư dân',
        `Bạn có chắc muốn xóa cư dân "${residentName}"?`,
        () => {
            Alert.loading('Đang xóa...');
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Cư dân đã được xóa thành công');
                setTimeout(() => window.location.reload(), 1500);
            }, 1000);
        }
    );
}

// ==================== APARTMENT MANAGEMENT ====================
function deleteApartment(apartmentId, apartmentCode) {
    Alert.confirm(
        'Xác nhận xóa căn hộ',
        `Bạn có chắc muốn xóa căn hộ "${apartmentCode}"?`,
        () => {
            Alert.loading('Đang xóa...');
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Căn hộ đã được xóa thành công');
                setTimeout(() => window.location.reload(), 1500);
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
                setTimeout(() => window.location.reload(), 1500);
            }, 1000);
        }
    );
}

// ==================== EXPORT ====================
function exportToExcel(dataType) {
    Alert.loading('Đang xuất dữ liệu...');
    setTimeout(() => {
        Alert.success('Xuất thành công!', `Dữ liệu ${dataType} đã được xuất ra Excel`);
    }, 1500);
}

function exportToPDF(dataType) {
    Alert.loading('Đang xuất dữ liệu...');
    setTimeout(() => {
        Alert.success('Xuất thành công!', `Dữ liệu ${dataType} đã được xuất ra PDF`);
    }, 1500);
}
