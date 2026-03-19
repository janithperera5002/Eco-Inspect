export const API_BASE = '/api';

export const URGENCY = {
  critical: { label: 'Critical', 
               badgeClass: 'badge-critical' },
  high:     { label: 'High',     
               badgeClass: 'badge-high' },
  medium:   { label: 'Medium',   
               badgeClass: 'badge-medium' },
  low:      { label: 'Low',      
               badgeClass: 'badge-low' }
};

export const STATUS = {
  received:       { label: 'Received',       
                    badgeClass: 'badge-received' },
  ai_processing:  { label: 'AI Processing',  
                    badgeClass: 'badge-processing' },
  pending_review: { label: 'Pending Review', 
                    badgeClass: 'badge-pending' },
  assigned:       { label: 'Assigned',       
                    badgeClass: 'badge-assigned' },
  in_progress:    { label: 'In Progress',    
                    badgeClass: 'badge-in-progress' },
  resolved:       { label: 'Resolved',       
                    badgeClass: 'badge-resolved' },
  rejected:       { label: 'Rejected',       
                    badgeClass: 'badge-rejected' }
};

export const CATEGORIES = {
  illegal_waste_disposal: 'Illegal Waste Disposal',
  water_pollution:        'Water Pollution',
  air_pollution:          'Air Pollution',
  deforestation:          'Deforestation',
  noise_pollution:        'Noise Pollution',
  chemical_spill:         'Chemical Spill',
  soil_contamination:     'Soil Contamination',
  wildlife_harm:          'Wildlife Harm',
  sewage_discharge:       'Sewage Discharge',
  other:                  'Other'
};
