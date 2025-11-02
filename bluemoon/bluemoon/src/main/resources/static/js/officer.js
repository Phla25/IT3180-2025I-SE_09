// ==================== OFFICER SPECIFIC JAVASCRIPT ====================

console.log('BlueMoon Portal - Officer Module Loaded ✓');

// ==================== APARTMENT MANAGEMENT ====================
function viewApartmentInfo(apartmentId) {
    Alert.loading('Đang tải thông tin căn hộ...');
    setTimeout(() => {
        Alert.close();
        window.location.href = `/officer/apartments/${apartmentId}`;
    }, 500);
}

function updateApartmentStatus(apartmentId, newStatus) {
    const statusLabels = {
        'occupied': 'Đang ở',
        'vacant': 'Trống',
        'maintenance': 'Bảo trì'
    };
    
    Alert.confirm(
        'Xác nhận cập nhật',
        `Cập nhật trạng thái căn hộ thành "${statusLabels[newStatus]}"?`,
        () => {
            Alert.loading('Đang cập nhật...');
            setTimeout(() => {
                Alert.success('Thành công!', 'Trạng thái căn hộ đã được cập nhật');
                setTimeout(() => window.location.reload(), 1500);
            }, 1000);
        }
    );
}

// ==================== HOUSEHOLD MANAGEMENT ====================
function viewHouseholdDetails(householdId) {
    Alert.loading('Đang tải thông tin hộ khẩu...');
    setTimeout(() => {
        Alert.close();
        window.location.href = `/officer/households/${householdId}`;
    }, 500);
}

function deleteHousehold(householdId, householdCode) {
    Alert.confirm(
        'Xác nhận xóa hộ khẩu',
        `Bạn có chắc muốn xóa hộ khẩu "${householdCode}"?`,
        () => {
            Alert.loading('Đang xóa...');
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Hộ khẩu đã được xóa thành công');
                setTimeout(() => window.location.reload(), 1500);
            }, 1000);
        }
    );
}

// ==================== GENERAL ASSET MANAGEMENT ====================
function deleteAsset(assetId, assetName) {
    Alert.confirm(
        'Xác nhận xóa tài sản',
        `Bạn có chắc muốn xóa tài sản "${assetName}"?`,
        () => {
            Alert.loading('Đang xóa...');
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Tài sản đã được xóa thành công');
                setTimeout(() => window.location.reload(), 1500);
            }, 1000);
        }
    );
}

// ==================== REPORTS ====================
function generateOfficerReport(reportType) {
    Alert.loading('Đang tạo báo cáo...');
    setTimeout(() => {
        Alert.success('Báo cáo đã sẵn sàng!', `Báo cáo ${reportType} đã được tạo`);
    }, 2000);
}
