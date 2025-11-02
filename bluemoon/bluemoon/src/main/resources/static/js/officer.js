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
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
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

function addHouseholdMember(householdId) {
    window.location.href = `/officer/households/${householdId}/add-member`;
}

function changeHouseholdOwner(householdId) {
    Alert.confirm(
        'Xác nhận thay đổi chủ hộ',
        'Bạn có chắc muốn thay đổi chủ hộ? Hành động này sẽ thay đổi thông tin pháp lý của hộ khẩu.',
        () => {
            window.location.href = `/officer/households/${householdId}/change-owner`;
        }
    );
}

function splitHousehold(householdId) {
    Alert.confirm(
        'Xác nhận tách hộ',
        'Bạn có chắc muốn tách hộ khẩu này? Hành động này không thể hoàn tác.',
        () => {
            window.location.href = `/officer/households/${householdId}/split`;
        }
    );
}

function deleteHousehold(householdId, householdCode) {
    Alert.confirm(
        'Xác nhận xóa hộ khẩu',
        `Bạn có chắc muốn xóa hộ khẩu "${householdCode}"? Hành động này không thể hoàn tác.`,
        () => {
            Alert.loading('Đang xóa...');
            
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Hộ khẩu đã được xóa thành công');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== RESIDENT MANAGEMENT ====================
function viewResidentInfo(residentId) {
    Alert.loading('Đang tải thông tin cư dân...');
    
    setTimeout(() => {
        Alert.close();
        window.location.href = `/officer/residents/${residentId}`;
    }, 500);
}

function verifyResidentDocument(residentId, documentType) {
    Alert.confirm(
        'Xác nhận xác thực',
        `Xác thực ${documentType} của cư dân này?`,
        () => {
            Alert.loading('Đang xử lý...');
            
            setTimeout(() => {
                Alert.success('Đã xác thực!', `${documentType} đã được xác thực thành công`);
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== GENERAL ASSET MANAGEMENT ====================
function viewAssetDetails(assetId) {
    Alert.loading('Đang tải thông tin tài sản...');
    
    setTimeout(() => {
        Alert.close();
        window.location.href = `/officer/assets/${assetId}`;
    }, 500);
}

function updateAssetStatus(assetId, newStatus) {
    const statusLabels = {
        'active': 'Đang hoạt động',
        'maintenance': 'Bảo trì',
        'broken': 'Hỏng hóc',
        'retired': 'Ngừng sử dụng'
    };
    
    Alert.confirm(
        'Xác nhận cập nhật',
        `Cập nhật trạng thái tài sản thành "${statusLabels[newStatus]}"?`,
        () => {
            Alert.loading('Đang cập nhật...');
            
            setTimeout(() => {
                Alert.success('Thành công!', 'Trạng thái tài sản đã được cập nhật');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

function deleteAsset(assetId, assetName) {
    Alert.confirm(
        'Xác nhận xóa tài sản',
        `Bạn có chắc muốn xóa tài sản "${assetName}"?`,
        () => {
            Alert.loading('Đang xóa...');
            
            setTimeout(() => {
                Alert.success('Đã xóa!', 'Tài sản đã được xóa thành công');
                setTimeout(() => {
                    window.location.reload();
                }, 1500);
            }, 1000);
        }
    );
}

// ==================== STATISTICS & REPORTS ====================
function generateOfficerReport(reportType) {
    Alert.loading('Đang tạo báo cáo...');
    
    setTimeout(() => {
        Alert.success('Báo cáo đã sẵn sàng!', `Báo cáo ${reportType} đã được tạo thành công`);
    }, 2000);
}

function refreshOfficerStats() {
    Alert.loading('Đang cập nhật thống kê...');
    
    setTimeout(() => {
        Alert.close();
        showNotification('Thống kê đã được cập nhật', 'success');
    }, 1000);
}

// ==================== OFFICER INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    console.log('Officer dashboard initialized');
    
    // Format all date values
    const dateElements = document.querySelectorAll('.date-format');
    dateElements.forEach(element => {
        const dateValue = element.textContent;
        if (dateValue) {
            element.textContent = formatDate(dateValue);
        }
    });
});

// ==================== FORM VALIDATION ====================
function validateHouseholdForm() {
    const requiredFields = ['apartmentId', 'ownerName', 'ownerCCCD', 'moveInDate'];
    let isValid = true;
    
    requiredFields.forEach(fieldId => {
        const field = document.getElementById(fieldId);
        if (field && !field.value.trim()) {
            isValid = false;
            field.classList.add('error');
        }
    });
    
    if (!isValid) {
        Alert.error('Lỗi', 'Vui lòng điền đầy đủ thông tin bắt buộc');
    }
    
    return isValid;
}
